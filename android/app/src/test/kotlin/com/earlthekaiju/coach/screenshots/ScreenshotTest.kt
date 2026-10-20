package com.earlthekaiju.coach.screenshots

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.designsystem.component.KaijuNavRail
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.data.prefs.AppSettings
import com.earlthekaiju.coach.data.repo.SessionDraft
import com.earlthekaiju.coach.data.repo.StudentDraft
import com.earlthekaiju.coach.designsystem.component.KaijuBottomBar
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuTheme
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.timer.TimerEngine
import com.earlthekaiju.coach.domain.timer.TimerProgram
import com.earlthekaiju.coach.feature.attendance.AttendanceScreen
import com.earlthekaiju.coach.feature.attendance.AttendanceActions
import com.earlthekaiju.coach.feature.attendance.AttendanceUiState
import com.earlthekaiju.coach.feature.classmode.ClassModeActions
import com.earlthekaiju.coach.feature.classmode.ClassModeScreen
import com.earlthekaiju.coach.feature.classmode.ClassModeUiState
import com.earlthekaiju.coach.feature.classmode.ClassSegmentUi
import com.earlthekaiju.coach.feature.classmode.PostClassScreen
import com.earlthekaiju.coach.feature.classmode.PostClassUiState
import com.earlthekaiju.coach.feature.lessons.LessonEditorActions
import com.earlthekaiju.coach.feature.lessons.LessonEditorScreen
import com.earlthekaiju.coach.feature.lessons.LessonEditorUiState
import com.earlthekaiju.coach.feature.library.LibraryActions
import com.earlthekaiju.coach.feature.library.LibraryScreen
import com.earlthekaiju.coach.feature.library.LibraryUiState
import com.earlthekaiju.coach.feature.planner.DetailSegment
import com.earlthekaiju.coach.feature.planner.PlannerActions
import com.earlthekaiju.coach.feature.planner.PlannerScreen
import com.earlthekaiju.coach.feature.planner.PlannerUiState
import com.earlthekaiju.coach.feature.planner.SessionDetailActions
import com.earlthekaiju.coach.feature.planner.SessionDetailScreen
import com.earlthekaiju.coach.feature.planner.SessionDetailUiState
import com.earlthekaiju.coach.feature.planner.SessionEditorActions
import com.earlthekaiju.coach.feature.planner.SessionEditorScreen
import com.earlthekaiju.coach.feature.planner.SessionEditorUiState
import com.earlthekaiju.coach.feature.planner.weekStartOf
import com.earlthekaiju.coach.feature.settings.ProgramsScreen
import com.earlthekaiju.coach.feature.settings.SettingsActions
import com.earlthekaiju.coach.feature.settings.SettingsScreen
import com.earlthekaiju.coach.feature.settings.SettingsUiState
import com.earlthekaiju.coach.feature.students.StudentDetailScreen
import com.earlthekaiju.coach.feature.students.StudentDetailUiState
import com.earlthekaiju.coach.feature.students.StudentEditorActions
import com.earlthekaiju.coach.feature.students.StudentEditorScreen
import com.earlthekaiju.coach.feature.students.StudentEditorUiState
import com.earlthekaiju.coach.feature.students.StudentsActions
import com.earlthekaiju.coach.feature.students.StudentsScreen
import com.earlthekaiju.coach.feature.students.StudentsUiState
import com.earlthekaiju.coach.feature.timer.TimerActions
import com.earlthekaiju.coach.feature.timer.TimerScreen
import com.earlthekaiju.coach.feature.timer.TimerState
import com.earlthekaiju.coach.feature.timer.TimerUiState
import com.earlthekaiju.coach.feature.timer.toProgram
import com.earlthekaiju.coach.feature.today.SegmentPreview
import com.earlthekaiju.coach.feature.today.TodayActions
import com.earlthekaiju.coach.feature.today.TodayScreen
import com.earlthekaiju.coach.feature.today.TodayUiState
import com.earlthekaiju.coach.navigation.TopLevel
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime

private const val PHONE = "w412dp-h915dp-xxhdpi"
private const val PHONE_TALL = "w412dp-h1700dp-xxhdpi"
private const val PHONE_LANDSCAPE = "w915dp-h412dp-land-xxhdpi"
private const val SHEET = "w412dp-h2900dp-xxhdpi"
private const val TABLET = "w900dp-h1280dp-xhdpi"

/**
 * Renders every Sprint 01 screen from real Compose code with fixed fictional data.
 * Run with `./gradlew recordRoborazziDebug`; images land in app/build/outputs/roborazzi/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, fontScale: Float = 1f, cropToContent: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                KaijuTheme(reduceMotion = true) {
                    Box(Modifier.fillMaxSize().background(Kaiju.colors.background)) {
                        if (cropToContent) {
                            Box(Modifier.fillMaxWidth().wrapContentHeight().testTag("content")) { content() }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val path = "build/outputs/roborazzi/$name.png"
        if (cropToContent) compose.onNodeWithTag("content").captureRoboImage(path) else compose.onRoot().captureRoboImage(path)
    }

    /** Top-level screens are shown inside the real bottom navigation bar. */
    @Composable
    private fun Frame(tab: TopLevel, content: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { content() }
            KaijuBottomBar(TopLevel.entries.map { it.item }, tab.ordinal, {})
        }
    }

    private val guardPreview = Fx.guardRetentionSegments.map { (t, m, k) -> SegmentPreview(t, m, k.label) }

    private fun today(fontScale: Float = 1f, name: String = "01-today") = snap(name, fontScale) {
        Frame(TopLevel.TODAY) {
            TodayScreen(
                TodayUiState(
                    loading = false, now = Fx.now, featured = Fx.nextClass,
                    featuredObjective = Fx.lessonDraft.objective, featuredSegments = guardPreview,
                    otherToday = listOf(Fx.kidsDone, Fx.openMat), todayCount = 3, lessonPlanCount = 3,
                ),
                TodayActions(),
            )
        }
    }

    @Test fun s01_today() = today()

    @Config(qualifiers = PHONE_TALL)
    @Test fun s01b_today_full() = today(name = "01b-today-full")

    @Test fun s01c_today_live() = snap("01c-today-live") {
        Frame(TopLevel.TODAY) {
            TodayScreen(
                TodayUiState(
                    loading = false, now = Fx.now.withHour(18).withMinute(46), featured = Fx.liveClass,
                    featuredSegments = guardPreview, otherToday = listOf(Fx.kidsDone, Fx.openMat), todayCount = 3, lessonPlanCount = 3,
                ),
                TodayActions(),
            )
        }
    }

    @Test fun s01d_today_empty() = snap("01d-today-empty") {
        Frame(TopLevel.TODAY) { TodayScreen(TodayUiState(loading = false, now = Fx.now, lessonPlanCount = 3), TodayActions()) }
    }

    @Test fun s01e_today_font200() = today(fontScale = 2f, name = "01e-today-font-200")

    private fun weekSessions() = run {
        val mon = weekStartOf(Fx.today)
        listOf(
            Fx.summary(Fx.session("w1", "Fundamentals", mon, LocalTime.of(18, 30), 60, SessionStatus.COMPLETED), present = 7),
            Fx.kidsDone, Fx.nextClass, Fx.openMat,
            Fx.summary(Fx.session("w3", "Fundamentals", mon.plusDays(2), LocalTime.of(18, 30), 60)),
            Fx.summary(Fx.session("w4", "Kids 6–12", mon.plusDays(3), LocalTime.of(17, 0), 45, programId = "p-kids", lessonPlanId = "l-kids"), "Kids 6–12", com.earlthekaiju.coach.domain.model.ProgramColor.ACCENT, "Safe Falls & Shrimping", 4),
            Fx.summary(Fx.session("w5", "Holiday open mat", mon.plusDays(4), LocalTime.of(18, 0), 60, SessionStatus.CANCELED, lessonPlanId = null), lesson = null),
            Fx.summary(Fx.session("w6", "All Levels", mon.plusDays(5), LocalTime.of(10, 0), 75, programId = "p-all", lessonPlanId = "l-closed"), "Adult All Levels", com.earlthekaiju.coach.domain.model.ProgramColor.GOLD, "Closed Guard Sweeps", 5),
        )
    }

    @Test fun s02_planner_week() = snap("02-planner-week") {
        Frame(TopLevel.PLANNER) {
            PlannerScreen(
                PlannerUiState(Fx.today, weekStartOf(Fx.today), Fx.today, weekSessions = weekSessions(), lessons = Fx.lessons, loading = false),
                PlannerActions(),
            )
        }
    }

    @Test fun s02b_planner_lessons() = snap("02b-planner-lessons") {
        Frame(TopLevel.PLANNER) {
            PlannerScreen(
                PlannerUiState(Fx.today, weekStartOf(Fx.today), Fx.today, showLessons = true, weekSessions = weekSessions(), lessons = Fx.lessons, loading = false),
                PlannerActions(),
            )
        }
    }

    @Test fun s02c_session_editor() = snap("02c-session-editor") {
        SessionEditorScreen(
            SessionEditorUiState(
                loading = false, isNew = true,
                draft = SessionDraft(programId = "p-adults", lessonPlanId = "l-guard", date = Fx.today.plusDays(1), startTime = LocalTime.of(18, 30), durationMinutes = 60, location = "Main mat"),
                programs = Fx.programs, lessons = Fx.lessons,
            ),
            SessionEditorActions(),
        )
    }

    @Test fun s02d_session_detail() = snap("02d-session-detail") {
        SessionDetailScreen(
            SessionDetailUiState(
                loading = false,
                summary = Fx.nextClass,
                objective = Fx.lessonDraft.objective,
                segments = Fx.lessonDraft.segments.map { s ->
                    DetailSegment(s.title, s.minutes, s.kind.label, s.techniqueIds.mapNotNull { id -> Fx.techniques.firstOrNull { it.technique.id == id }?.technique?.name }, s.notes, false)
                },
            ),
            SessionDetailActions(),
        )
    }

    private fun lessonBuilder(name: String) = snap(name) {
        LessonEditorScreen(
            LessonEditorUiState(loading = false, isNew = false, draft = Fx.lessonDraft, programs = Fx.programs, techniques = Fx.techniques),
            LessonEditorActions(),
        )
    }

    @Test fun s03_lesson_builder() = lessonBuilder("03-lesson-builder")

    @Config(qualifiers = SHEET)
    @Test fun s03b_lesson_builder_full() = lessonBuilder("03b-lesson-builder-full")

    private val classSegments = Fx.lessonDraft.segments.map { s ->
        ClassSegmentUi(s.title, s.minutes, s.kind.label, s.techniqueIds.mapNotNull { id -> Fx.techniques.firstOrNull { it.technique.id == id }?.technique?.name }, s.notes)
    }

    private fun classMode(name: String, elapsedSec: Long = 340, fontScale: Float = 1f, withTimer: Boolean = false) = snap(name, fontScale) {
        ClassModeScreen(
            ClassModeUiState(
                loading = false, title = "Fundamentals", programName = "Adult Fundamentals", status = SessionStatus.IN_PROGRESS,
                segments = classSegments, currentIndex = 1, segmentElapsedMillis = elapsedSec * 1000, classElapsedMillis = (600 + elapsedSec) * 1000,
                presentCount = 6, expectedCount = 8, wallTime = LocalTime.of(18, 46),
                timer = if (withTimer) timerState(10_000 + 300_000 + 60_000 + 41_000) else null,
            ),
            ClassModeActions(),
        )
    }

    @Test fun s04_class_mode() = classMode("04-class-mode")
    @Test fun s04b_class_mode_overtime() = classMode("04b-class-mode-overtime", elapsedSec = 1_280)
    @Test fun s04c_class_mode_with_timer() = classMode("04c-class-mode-with-timer", withTimer = true)

    @Config(qualifiers = PHONE_LANDSCAPE)
    @Test fun s04d_class_mode_landscape() = classMode("04d-class-mode-landscape")

    @Test fun s04e_class_mode_font150() = classMode("04e-class-mode-font-150", fontScale = 1.5f)

    @Test fun s04f_post_class() = snap("04f-post-class") {
        PostClassScreen(
            PostClassUiState(
                title = "Fundamentals · Guard Retention", programName = "Adult Fundamentals", actualMinutes = 62, plannedMinutes = 60,
                presentCount = 6, expectedCount = 8, segments = classSegments.map { it.title to it.minutes }, segmentsDone = 4, notes = Fx.notes,
            ),
            onSaveNote = {}, onDone = {}, onAttendance = {},
        )
    }

    private val sparring: TimerProgram = Fx.presets[0].toProgram()

    private fun timerState(atMillis: Long?, pauseAt: Long? = null): TimerState {
        val snapshot = if (atMillis == null) {
            TimerEngine.idle(sparring)
        } else {
            var run = TimerEngine.start(sparring, 0)
            if (pauseAt != null) run = TimerEngine.pause(run, pauseAt)
            TimerEngine.snapshot(run, atMillis)
        }
        return TimerState(snapshot, sparring, "tp1", "Sparring rounds")
    }

    private fun timer(name: String, state: TimerState) = snap(name) {
        Frame(TopLevel.TIMER) {
            TimerScreen(TimerUiState(state, Fx.presets, LocalTime.of(18, 52), sound = true), TimerActions())
        }
    }

    @Test fun s05_timer_work() = timer("05-timer-work", timerState(10_000 + 300_000 + 60_000 + 41_000))
    @Test fun s05b_timer_rest() = timer("05b-timer-rest", timerState(10_000 + 300_000 + 22_000))
    @Test fun s05c_timer_paused() = timer("05c-timer-paused", timerState(200_000, pauseAt = 150_000))
    @Test fun s05d_timer_ready() = timer("05d-timer-ready", timerState(null))
    @Test fun s05e_timer_warning() = timer("05e-timer-warning", timerState(10_000 + 272_000))
    @Test fun s05f_timer_finished() = timer("05f-timer-finished", timerState(sparring.totalMillis + 1_000))

    @Config(qualifiers = PHONE_LANDSCAPE)
    @Test fun s05g_timer_landscape() = snap("05g-timer-landscape") {
        TimerScreen(TimerUiState(timerState(10_000 + 300_000 + 60_000 + 41_000), Fx.presets, LocalTime.of(18, 52)), TimerActions())
    }

    @Test fun s06_students() = snap("06-students") {
        Frame(TopLevel.STUDENTS) { StudentsScreen(StudentsUiState(loading = false, programs = Fx.programs, all = Fx.students), StudentsActions()) }
    }

    @Test fun s06b_student_detail() = snap("06b-student-detail") {
        StudentDetailScreen(
            StudentDetailUiState(loading = false, student = Fx.students[0].student, programs = Fx.programs.take(2), history = Fx.history, today = Fx.today),
            onBack = {}, onEdit = {}, onOpenSession = {},
        )
    }

    @Test fun s06c_student_editor() = snap("06c-student-editor") {
        StudentEditorScreen(
            StudentEditorUiState(
                loading = false, isNew = false,
                draft = StudentDraft("st1", "Ana", "Ribeiro", Belt.BLUE, 2, notes = "Working on knee shield retention.", programIds = setOf("p-adults", "p-all")),
                programs = Fx.programs,
            ),
            StudentEditorActions(),
        )
    }

    @Test fun s06d_students_empty() = snap("06d-students-empty") {
        Frame(TopLevel.STUDENTS) { StudentsScreen(StudentsUiState(loading = false, programs = Fx.programs), StudentsActions()) }
    }

    private fun attendance(name: String, fontScale: Float = 1f) = snap(name, fontScale) {
        AttendanceScreen(
            AttendanceUiState(
                loading = false, session = Fx.liveClass, roster = Fx.roster,
                others = Fx.students.filter { s -> Fx.roster.none { it.studentId == s.student.id } },
            ),
            AttendanceActions(),
        )
    }

    @Test fun s07_attendance() = attendance("07-attendance")
    @Test fun s07b_attendance_font150() = attendance("07b-attendance-font-150", 1.5f)

    @Test fun s08_library() = snap("08-library") {
        Frame(TopLevel.LIBRARY) { LibraryScreen(LibraryUiState(loading = false, techniques = Fx.techniques), LibraryActions()) }
    }

    @Test fun s08b_library_media_concept() = snap("08b-library-media-concept") {
        Frame(TopLevel.LIBRARY) { LibraryScreen(LibraryUiState(loading = false, showMedia = true, techniques = Fx.techniques), LibraryActions()) }
    }

    @Test fun s09_settings() = snap("09-settings") {
        SettingsScreen(SettingsUiState(settings = AppSettings(), programCount = 3, versionName = "0.1.0", showDebugTools = false), SettingsActions())
    }

    @Config(qualifiers = PHONE_TALL)
    @Test fun s09b_settings_full() = snap("09b-settings-full") {
        SettingsScreen(SettingsUiState(settings = AppSettings(), programCount = 3, versionName = "0.1.0", showDebugTools = false), SettingsActions())
    }

    @Test fun s09c_programs() = snap("09c-programs") {
        ProgramsScreen(Fx.programs, onBack = {}, onSave = {}, onDelete = {})
    }

    @Config(qualifiers = SHEET)
    @Test fun s10_design_foundations() = snap("10-design-foundations", cropToContent = true) { FoundationsSheet() }

    @Config(qualifiers = SHEET)
    @Test fun s11_design_components() = snap("11-design-components", cropToContent = true) { ComponentsSheet() }

    @Test fun s02e_planner_font150() = snap("02e-planner-font-150", fontScale = 1.5f) {
        Frame(TopLevel.PLANNER) {
            PlannerScreen(
                PlannerUiState(Fx.today, weekStartOf(Fx.today), Fx.today, weekSessions = weekSessions(), lessons = Fx.lessons, loading = false),
                PlannerActions(),
            )
        }
    }

    @Test fun s06e_students_font200() = snap("06e-students-font-200", fontScale = 2f) {
        Frame(TopLevel.STUDENTS) { StudentsScreen(StudentsUiState(loading = false, programs = Fx.programs, all = Fx.students), StudentsActions()) }
    }

    /** Tablet / wide window: the side rail replaces the bottom bar and content keeps a readable width. */
    @Config(qualifiers = TABLET)
    @Test fun s12_tablet_today() = snap("12-tablet-today") {
        Row(Modifier.fillMaxSize()) {
            KaijuNavRail(TopLevel.entries.map { it.item }, 0, {}, header = {
                Image(painterResource(R.drawable.brand_eyes_mark), null, Modifier.size(width = 40.dp, height = 32.dp))
            })
            Box(Modifier.weight(1f)) {
                TodayScreen(
                    TodayUiState(
                        loading = false, now = Fx.now, featured = Fx.nextClass,
                        featuredObjective = Fx.lessonDraft.objective, featuredSegments = guardPreview,
                        otherToday = listOf(Fx.kidsDone, Fx.openMat), todayCount = 3, lessonPlanCount = 3,
                    ),
                    TodayActions(),
                )
            }
        }
    }
}
