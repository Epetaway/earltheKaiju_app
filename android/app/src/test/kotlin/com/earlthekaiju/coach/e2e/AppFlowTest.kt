package com.earlthekaiju.coach.e2e

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.KaijuCoachApp
import com.earlthekaiju.coach.MainActivity
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.feature.timer.CuePlayer
import com.earlthekaiju.coach.domain.timer.TimerCue
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Wednesday, October 21, 2026, 6:20 PM in New Jersey — ten minutes before the sample Fundamentals class. */
class FixedClock : AppClock {
    private val zone = ZoneId.of("America/New_York")
    private val start = LocalDateTime.of(2026, 10, 21, 18, 20).atZone(zone).toInstant().toEpochMilli()
    override fun now(): Long = start
    override fun elapsedRealtime(): Long = 1_000_000L
    override fun zone(): ZoneId = zone
}

/** The real app, with a fixed clock, an in-memory database and silent cues. */
class E2eApp : KaijuCoachApp() {
    override fun createContainer(): AppContainer = AppContainer(
        context = this,
        clock = FixedClock(),
        database = KaijuDatabase.inMemory(this),
        cuePlayer = object : CuePlayer {
            override fun play(cue: TimerCue, sound: Boolean, vibration: Boolean) = Unit
        },
        bootCounter = { 1 },
    )
}

/**
 * Drives the whole app the way Earl would: load sample data, start the next class from Today,
 * step through every segment, finish, save, then use the timer and open a student.
 * Exercises MainActivity, navigation, ViewModels, repositories and Room together.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = E2eApp::class, qualifiers = "w412dp-h915dp-xxhdpi")
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Waits for [text] (exact case) to appear. On timeout, fails with the current semantics tree
     * and saves a screenshot, so a CI failure shows what was actually on screen.
     */
    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = 10_000) {
        try {
            compose.waitUntil(timeout) {
                compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: ComposeTimeoutException) {
            runCatching { shot("13x-failure") }
            val tree = runCatching { compose.onRoot().printToString(maxDepth = 40) }.getOrElse { "(tree unavailable: $it)" }
            throw AssertionError("Timed out waiting for \"$text\". Screen:\n$tree", e)
        }
    }

    /** A tappable element with exactly this label (a button or a nav item, never a badge). */
    private fun tap(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    @Test
    fun teachAClassEndToEnd() {
        // First launch: empty Today.
        waitForText("TODAY")
        waitForText("No class on the schedule")

        // Debug build: load the fictional sample data from Settings.
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNode(hasText("Load sample data") and hasClickAction()).performScrollTo().performClick()
        waitForText("Sample data loaded")
        compose.onNodeWithContentDescription("Back").performClick()

        // Today now features tonight's Fundamentals class.
        waitForText("Start class")
        waitForText("GUARD RETENTION")
        waitForText("Starts in 10 min")
        shot("13a-app-today-sample-data")

        // Start it: Class Mode opens on segment 01.
        compose.onAllNodes(hasText("Start class") and hasClickAction()).onFirst().performClick()
        waitForText("Next segment")
        waitForText("MOVEMENT PREPARATION")
        shot("13b-app-class-mode")

        // Step through the remaining segments, waiting for each to become current.
        for (title in listOf("GUARD RETENTION CONCEPTS", "PARTNER DRILLING", "POSITIONAL SPARRING")) {
            tap("Next segment")
            waitForText(title)
        }
        waitForText("Finish class")

        // Finish (hero button, then the confirm dialog).
        compose.onNode(hasText("Finish class") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        waitForText("Keep teaching")
        compose.onNode(hasText("Finish class") and hasAnyAncestor(isDialog())).performClick()
        waitForText("COMPLETE", substring = true)
        shot("13c-app-post-class")
        tap("Done")

        // Timer tab: start a round.
        waitForText("TODAY")
        tap("Timer")
        waitForText("Start")
        tap("Start")
        waitForText("Pause")
        shot("13d-app-timer-running")
        compose.onNodeWithContentDescription("Reset timer").performClick()

        // Students tab: the roster and a student's history.
        tap("Students")
        waitForText("Ana Ribeiro")
        tap("Ana Ribeiro")
        waitForText("ATTENDANCE HISTORY")
        waitForText("Blue belt", substring = true)
        shot("13e-app-student-detail")
    }
}
