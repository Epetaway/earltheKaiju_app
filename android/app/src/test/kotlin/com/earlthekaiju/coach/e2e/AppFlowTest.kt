package com.earlthekaiju.coach.e2e

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.KaijuCoachApp
import com.earlthekaiju.coach.MainActivity
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.prefs.SettingsRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
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

/** The real app, with a fixed clock, an in-memory database, fresh preferences and silent cues. */
class E2eApp : KaijuCoachApp() {
    override fun createContainer(): AppContainer = AppContainer(
        context = this,
        clock = FixedClock(),
        database = KaijuDatabase.inMemory(this),
        // A new preferences file per test: the app's DataStore is a process-wide singleton.
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { File(cacheDir, "e2e-${System.nanoTime()}.preferences_pb") },
            ),
        ),
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
            fail("Timed out waiting for \"$text\"", e)
        }
    }

    /** Waits until no node shows [text] (a sheet or dialog has finished closing). */
    private fun waitGone(text: String, timeout: Long = 10_000) {
        try {
            compose.waitUntil(timeout) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
        } catch (e: ComposeTimeoutException) {
            fail("\"$text\" never went away", e)
        }
    }

    private fun waitForDescription(description: String, timeout: Long = 10_000) {
        try {
            compose.waitUntil(timeout) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            fail("Timed out waiting for description \"$description\"", e)
        }
    }

    /** Fails with every window's semantics tree (screens, sheets, dialogs) and a screenshot. */
    private fun fail(message: String, cause: Throwable): Nothing {
        runCatching { shot("13x-failure") }
        val tree = runCatching {
            val roots = compose.onAllNodes(isRoot())
            (0 until roots.fetchSemanticsNodes().size).joinToString("\n--- window ---\n") { roots[it].printToString(maxDepth = 40) }
        }.getOrElse { "(tree unavailable: $it)" }
        throw AssertionError("$message. Screen:\n$tree", cause)
    }

    /** Taps [label] once exactly one tappable copy is on screen (e.g. after a snackbar swap). */
    private fun tapWhenSingle(label: String, timeout: Long = 10_000) {
        val matcher = hasText(label) and hasClickAction()
        try {
            compose.waitUntil(timeout) { compose.onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
        } catch (e: ComposeTimeoutException) {
            fail("Expected one \"$label\"", e)
        }
        compose.onNode(matcher).performClick()
    }

    private fun SemanticsNodeInteraction.scrollIfNeeded() = apply { runCatching { performScrollTo() } }

    /** Types into the text field whose label is [label]. */
    private fun fill(label: String, text: String) {
        compose.onNode(hasContentDescription(label) and hasSetTextAction()).scrollIfNeeded().performTextInput(text)
    }

    /** Taps a control by its accessibility label (icon buttons, quick actions, pickers). */
    private fun tapDescription(description: String, substring: Boolean = false) {
        compose.onAllNodes(hasContentDescription(description, substring = substring) and hasClickAction()).onFirst().scrollIfNeeded().performClick()
    }

    /** A tappable element with exactly this label (a button or a nav item, never a badge). */
    private fun tap(label: String) = compose.onNode(hasText(label) and hasClickAction()).scrollIfNeeded().performClick()

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

    /**
     * The Sprint 01 vertical slice from an empty app, through the real UI only:
     * program → student → lesson plan (rename, add, reorder segments) → schedule it with the
     * program → start → timer → attendance with undo → every segment → finish with a note →
     * activity restart → the student's history shows the class.
     */
    @Test
    fun buildScheduleAndTeachFromAnEmptyApp() {
        waitForText("No class on the schedule")

        // A program, so the class knows who to expect.
        compose.onNodeWithContentDescription("Settings").performClick()
        tap("Programs")
        waitForText("No programs yet")
        tap("Add program")
        fill("Name", "Adult Fundamentals")
        tap("Save program")
        waitGone("Save program")
        waitForText("Adult Fundamentals")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        // A student in that program.
        tap("Students")
        compose.onAllNodes((hasContentDescription("Add student") or hasText("Add student")) and hasClickAction()).onFirst().performClick()
        fill("First name", "Sam")
        fill("Last name", "Rivera")
        tap("Adult Fundamentals")
        tap("Save")
        waitForText("Sam Rivera")

        // A lesson plan, started from Today's empty state.
        tap("Today")
        tap("Build a lesson plan")
        fill("Lesson title", "Guard Retention")
        tap("Untitled segment")
        fill("Title", "Positional sparring")
        tap("Done")
        waitGone("EDIT SEGMENT")
        tap("Add segment")
        fill("Title", "Guard retention concepts")
        tap("Done")
        waitGone("EDIT SEGMENT")
        tapDescription("Move segment 3 up")
        shot("14a-flow-lesson-builder")
        tap("Save and assign to a class")

        // Schedule it tonight with the program (the lesson is already attached).
        waitForDescription("Lesson plan: Guard Retention")
        tapDescription("Program:", substring = true)
        tap("Adult Fundamentals")
        waitForDescription("Program: Adult Fundamentals")
        shot("14b-flow-session-editor")
        tap("Schedule class")

        // Today now features it, with Sam expected.
        waitForText("GUARD RETENTION")
        waitForText("Starts in 10 min")
        shot("14c-flow-today-scheduled")

        // Teach: start, run the timer, take attendance, step through, finish.
        compose.onAllNodes(hasText("Start class") and hasClickAction()).onFirst().performClick()
        waitForText("WARM-UP")
        tapDescription("Timer")
        tap("Start")
        waitForText("Pause")
        tapDescription("Back to class")

        tapDescription("Attendance", substring = true)
        waitForText("Sam Rivera")
        tap("Present")
        waitForDescription("1 present, 0 absent, 0 not marked")
        tap("Absent")
        waitForDescription("0 present, 1 absent, 0 not marked")
        tapWhenSingle("Undo")
        waitForDescription("1 present, 0 absent, 0 not marked")
        shot("14d-flow-attendance")
        compose.onNodeWithContentDescription("Back").performClick()

        for (title in listOf("GUARD RETENTION CONCEPTS", "POSITIONAL SPARRING")) {
            tap("Next segment")
            waitForText(title)
        }
        compose.onNode(hasText("Finish class") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Finish class") and hasAnyAncestor(isDialog())).performClick()
        waitForText("COMPLETE", substring = true)
        fill("Add a note", "Frames landed. Sparring ran long.")
        tap("Done")
        waitForText("TODAY")

        // The activity restarts (rotation, theme change): everything is still there.
        compose.activityRule.scenario.recreate()
        tap("Students")
        tap("Sam Rivera")
        waitForText("ATTENDANCE HISTORY")
        waitForText("PRESENT")
        shot("14e-flow-student-history")
    }
}
