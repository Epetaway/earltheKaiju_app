package com.earlthekaiju.coach.device

import android.content.pm.ActivityInfo
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.earlthekaiju.coach.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Sprint 01 vertical slice on a real Android system (CI runs it on an API 35 Pixel 7
 * emulator), with Accessibility Test Framework checks on every interaction and full-screen
 * screenshots, system bars and keyboard included, saved to /data/local/tmp/kaiju.
 *
 * Mirrors the Robolectric AppFlowTest, which gives faster feedback but no real window,
 * insets, keyboard or accessibility service.
 */
@RunWith(AndroidJUnit4::class)
class DeviceFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val shotDir = "/data/local/tmp/kaiju"

    @Before fun setUp() {
        shell("mkdir -p $shotDir")
        // Label, contrast, touch-target and traversal checks run on every action below.
        compose.enableAccessibilityChecks()
    }

    @Test
    fun buildScheduleAndTeachOnADevice() {
        waitForText("No class on the schedule")
        shot("d01-today-empty")

        // Program.
        compose.onNodeWithContentDescription("Settings").performClick()
        tap("Programs")
        waitForText("No programs yet")
        tap("Add program")
        fill("Name", "Adult Fundamentals")
        tap("Save program")
        waitGone("Save program")
        waitForText("Adult Fundamentals")
        shot("d02-programs")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        // Student, with the on-screen keyboard up.
        tap("Students")
        compose.onAllNodes((hasContentDescription("Add student") or hasText("Add student")) and hasClickAction()).onFirst().performClick()
        fill("First name", "Sam")
        fill("Last name", "Rivera")
        shot("d03-student-editor-keyboard")
        tap("Adult Fundamentals")
        tap("Save")
        waitForText("Sam Rivera")

        // Lesson plan.
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
        shot("d04-lesson-builder")
        tap("Save and assign to a class")

        // Schedule tonight.
        waitForDescription("Lesson plan: Guard Retention")
        tapDescription("Program:", substring = true)
        tap("Adult Fundamentals")
        waitForDescription("Program: Adult Fundamentals")
        shot("d05-session-editor")
        tap("Schedule class")
        waitForText("GUARD RETENTION")
        waitForText("Starts in", substring = true)
        shot("d06-today-scheduled")

        // Teach.
        compose.onAllNodes(hasText("Start class") and hasClickAction()).onFirst().performClick()
        waitForText("WARM-UP")
        shot("d07-class-mode")
        rotate(landscape = true)
        waitForText("WARM-UP")
        shot("d08-class-mode-landscape")
        rotate(landscape = false)
        waitForText("WARM-UP")

        tapDescription("Timer", substring = true)
        tap("Start")
        waitForText("Pause")
        Thread.sleep(2_500) // let the real clock move the count-in
        shot("d09-class-timer-running")
        tapDescription("Back to class")

        tapDescription("Attendance", substring = true)
        waitForText("Sam Rivera")
        tap("Present")
        waitForDescription("1 present, 0 absent, 0 not marked")
        tap("Absent")
        waitForDescription("0 present, 1 absent, 0 not marked")
        tapWhenSingle("Undo")
        waitForDescription("1 present, 0 absent, 0 not marked")
        shot("d10-attendance")
        compose.onNodeWithContentDescription("Back").performClick()

        for (title in listOf("GUARD RETENTION CONCEPTS", "POSITIONAL SPARRING")) {
            tap("Next segment")
            waitForText(title)
        }
        compose.onNode(hasText("Finish class") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Finish class") and hasAnyAncestor(isDialog())).performClick()
        waitForText("COMPLETE", substring = true)
        fill("Add a note", "Frames landed. Sparring ran long.")
        shot("d11-post-class")
        tap("Done")
        waitForText("TODAY")

        // History survives an activity restart.
        compose.activityRule.scenario.recreate()
        tap("Students")
        tap("Sam Rivera")
        waitForText("ATTENDANCE HISTORY")
        waitForText("PRESENT")
        shot("d12-student-history")
        compose.onNodeWithContentDescription("Back").performClick()

        // The other main screens on a real device, with the fictional sample data.
        tap("Today")
        compose.onNodeWithContentDescription("Settings").performClick()
        tap("Load sample data")
        waitForText("Sample data loaded")
        shot("d13-settings")
        compose.onNodeWithContentDescription("Back").performClick()
        tap("Planner")
        waitForText("PLANNER")
        shot("d14-planner")
        tap("Students")
        waitForText("Ana Ribeiro")
        shot("d15-students")
        tap("Library")
        waitForText("LIBRARY")
        shot("d16-library")
        tap("Timer")
        waitForText("TIMER", substring = true)
        shot("d17-timer")
    }

    // --- helpers -------------------------------------------------------------------------

    private fun shell(command: String) {
        val pfd: ParcelFileDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() } // waits for completion
    }

    /** Full-screen capture of what the device shows, including system bars and keyboard. */
    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400) // let the window settle (keyboard, sheet scrims)
        shell("screencap -p $shotDir/$name.png")
    }

    private fun rotate(landscape: Boolean) {
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        compose.waitForIdle()
        Thread.sleep(800)
    }

    private fun waitForText(text: String, substring: Boolean = false, timeout: Long = 15_000) = waitFor("\"$text\"", timeout) {
        compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitGone(text: String, timeout: Long = 15_000) = waitFor("\"$text\" to go away", timeout) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    }

    private fun waitForDescription(description: String, timeout: Long = 15_000) = waitFor("description \"$description\"", timeout) {
        compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitFor(what: String, timeout: Long, condition: () -> Boolean) {
        try {
            compose.waitUntil(timeout, condition)
        } catch (e: ComposeTimeoutException) {
            runCatching { shell("screencap -p $shotDir/dx-failure.png") }
            val tree = runCatching {
                val roots = compose.onAllNodes(isRoot())
                (0 until roots.fetchSemanticsNodes().size).joinToString("\n--- window ---\n") { roots[it].printToString(maxDepth = 40) }
            }.getOrElse { "(tree unavailable: $it)" }
            throw AssertionError("Timed out waiting for $what. Screen:\n$tree", e)
        }
    }

    private fun SemanticsNodeInteraction.scrollIfNeeded() = apply { runCatching { performScrollTo() } }

    private fun tap(label: String) = compose.onNode(hasText(label) and hasClickAction()).scrollIfNeeded().performClick()

    private fun tapDescription(description: String, substring: Boolean = false) {
        compose.onAllNodes(hasContentDescription(description, substring = substring) and hasClickAction()).onFirst().scrollIfNeeded().performClick()
    }

    private fun tapWhenSingle(label: String) {
        val matcher = hasText(label) and hasClickAction()
        waitFor("exactly one \"$label\"", 15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().size == 1 }
        compose.onNode(matcher).performClick()
    }

    private fun fill(label: String, text: String) {
        compose.onNode(hasContentDescription(label) and hasSetTextAction()).scrollIfNeeded().performTextInput(text)
    }
}
