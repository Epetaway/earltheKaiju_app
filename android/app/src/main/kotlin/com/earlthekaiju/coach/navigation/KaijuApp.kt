package com.earlthekaiju.coach.navigation

import android.text.format.DateFormat
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.designsystem.component.BottomNavItem
import com.earlthekaiju.coach.designsystem.component.KaijuBottomBar
import com.earlthekaiju.coach.designsystem.component.KaijuNavRail
import com.earlthekaiju.coach.designsystem.component.KaijuSnackbar
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuTheme
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.LocalAppContainer
import com.earlthekaiju.coach.feature.attendance.AttendanceDestination
import com.earlthekaiju.coach.feature.classmode.ClassModeDestination
import com.earlthekaiju.coach.feature.classmode.PostClassDestination
import com.earlthekaiju.coach.feature.lessons.LessonEditorDestination
import com.earlthekaiju.coach.feature.library.LibraryDestination
import com.earlthekaiju.coach.feature.library.TechniqueEditorDestination
import com.earlthekaiju.coach.feature.planner.PlannerActions
import com.earlthekaiju.coach.feature.planner.PlannerDestination
import com.earlthekaiju.coach.feature.planner.SessionDetailActions
import com.earlthekaiju.coach.feature.planner.SessionDetailDestination
import com.earlthekaiju.coach.feature.planner.SessionEditorDestination
import com.earlthekaiju.coach.feature.settings.ProgramsDestination
import com.earlthekaiju.coach.feature.settings.SettingsDestination
import com.earlthekaiju.coach.feature.students.StudentDetailDestination
import com.earlthekaiju.coach.feature.students.StudentEditorDestination
import com.earlthekaiju.coach.feature.students.StudentsDestination
import com.earlthekaiju.coach.feature.timer.TimerDestination
import com.earlthekaiju.coach.feature.today.TodayActions
import com.earlthekaiju.coach.feature.today.TodayDestination
import com.earlthekaiju.coach.ui.LocalSnackbarHost
import com.earlthekaiju.coach.ui.LocalUse24Hour

/** The five primary destinations, in bottom-bar order. */
enum class TopLevel(
    val graph: Any,
    val item: BottomNavItem,
    private val isGraph: (NavDestination) -> Boolean,
    private val isStart: (NavDestination) -> Boolean,
) {
    TODAY(TodayGraph, BottomNavItem("Today", KaijuIcons.Today), { it.hasRoute<TodayGraph>() }, { it.hasRoute<TodayRoute>() }),
    PLANNER(PlannerGraph, BottomNavItem("Planner", KaijuIcons.Planner), { it.hasRoute<PlannerGraph>() }, { it.hasRoute<PlannerRoute>() }),
    TIMER(TimerGraph, BottomNavItem("Timer", KaijuIcons.Timer), { it.hasRoute<TimerGraph>() }, { it.hasRoute<TimerRoute>() }),
    STUDENTS(StudentsGraph, BottomNavItem("Students", KaijuIcons.Students), { it.hasRoute<StudentsGraph>() }, { it.hasRoute<StudentsRoute>() }),
    LIBRARY(LibraryGraph, BottomNavItem("Library", KaijuIcons.Library), { it.hasRoute<LibraryGraph>() }, { it.hasRoute<LibraryRoute>() }),
    ;

    /** True when [destination] is inside this tab's graph. */
    fun isIn(destination: NavDestination?): Boolean = destination?.hierarchy?.any(isGraph) == true

    /** True when [destination] is this tab's own screen (where the bottom bar shows). */
    fun isTopOf(destination: NavDestination): Boolean = isStart(destination)
}

@Composable
fun KaijuApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val use24h = DateFormat.is24HourFormat(LocalContext.current)
    CompositionLocalProvider(
        LocalAppContainer provides container,
        LocalSnackbarHost provides snackbar,
        LocalUse24Hour provides use24h,
    ) {
        KaijuTheme {
            val entry by navController.currentBackStackEntryAsState()
            val destination = entry?.destination
            val selected = TopLevel.entries.indexOfFirst { it.isIn(destination) }.coerceAtLeast(0)
            val showNav = destination != null && TopLevel.entries.any { it.isTopOf(destination) }
            val items = TopLevel.entries.map { it.item }
            val onSelect: (Int) -> Unit = { navController.navigateToTab(TopLevel.entries[it]) }
            // Side insets (3-button navigation and camera cutouts in landscape) apply app-wide;
            // top and bottom insets are handled by the bars and screens themselves.
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(Kaiju.colors.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                val wide = maxWidth >= 600.dp
                Scaffold(
                    containerColor = Kaiju.colors.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbar) { KaijuSnackbar(it) } },
                    bottomBar = { if (showNav && !wide) KaijuBottomBar(items, selected, onSelect) },
                ) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        if (showNav && wide) {
                            KaijuNavRail(items, selected, onSelect, header = {
                                Image(painterResource(R.drawable.brand_eyes_mark), contentDescription = null, modifier = Modifier.size(width = 40.dp, height = 32.dp))
                            })
                        }
                        KaijuNavHost(navController, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.graph) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun KaijuNavHost(nav: NavHostController, modifier: Modifier) {
    val reduceMotion = Kaiju.reduceMotion
    val enter: EnterTransition = if (reduceMotion) EnterTransition.None else fadeIn(tween(180))
    val exit: ExitTransition = if (reduceMotion) ExitTransition.None else fadeOut(tween(120))
    val back: () -> Unit = { nav.popBackStack() }
    val openSettings: () -> Unit = { nav.navigate(SettingsRoute) }

    NavHost(
        navController = nav,
        startDestination = TodayGraph,
        modifier = modifier,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        navigation<TodayGraph>(startDestination = TodayRoute) {
            composable<TodayRoute> {
                TodayDestination(
                    TodayActions(
                        onStartClass = { nav.navigate(ClassModeRoute(it)) },
                        onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
                        onAttendance = { nav.navigate(AttendanceRoute(it)) },
                        onScheduleClass = { nav.navigate(SessionEditorRoute()) },
                        onNewLesson = { nav.navigate(LessonEditorRoute()) },
                        onOpenLessons = {
                            nav.navigate(PlannerRoute(showLessons = true)) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onOpenSettings = openSettings,
                    ),
                )
            }
        }
        navigation<PlannerGraph>(startDestination = PlannerRoute()) {
            composable<PlannerRoute> {
                PlannerDestination(
                    PlannerActions(
                        onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
                        onSchedule = { date -> nav.navigate(SessionEditorRoute(date = date.toString())) },
                        onOpenLesson = { nav.navigate(LessonEditorRoute(it)) },
                        onNewLesson = { nav.navigate(LessonEditorRoute()) },
                        onOpenSettings = openSettings,
                    ),
                )
            }
        }
        navigation<TimerGraph>(startDestination = TimerRoute) {
            composable<TimerRoute> { TimerDestination() }
        }
        navigation<StudentsGraph>(startDestination = StudentsRoute) {
            composable<StudentsRoute> {
                StudentsDestination(
                    onOpen = { nav.navigate(StudentDetailRoute(it)) },
                    onAdd = { nav.navigate(StudentEditorRoute()) },
                    onOpenSettings = openSettings,
                )
            }
        }
        navigation<LibraryGraph>(startDestination = LibraryRoute) {
            composable<LibraryRoute> {
                LibraryDestination(
                    onOpenTechnique = { nav.navigate(TechniqueEditorRoute(it)) },
                    onAddTechnique = { nav.navigate(TechniqueEditorRoute()) },
                    onOpenSettings = openSettings,
                )
            }
        }

        composable<SessionDetailRoute> {
            SessionDetailDestination(
                SessionDetailActions(
                    onBack = back,
                    onStart = { nav.navigate(ClassModeRoute(it)) },
                    onAttendance = { nav.navigate(AttendanceRoute(it)) },
                    onEdit = { nav.navigate(SessionEditorRoute(sessionId = it)) },
                    onOpenLesson = { nav.navigate(LessonEditorRoute(it)) },
                ),
            )
        }
        composable<SessionEditorRoute> {
            SessionEditorDestination(onDone = back, onManagePrograms = { nav.navigate(ProgramsRoute) })
        }
        composable<LessonEditorRoute> {
            LessonEditorDestination(
                onClose = back,
                onAssign = { planId ->
                    nav.navigate(SessionEditorRoute(lessonPlanId = planId)) {
                        popUpTo<LessonEditorRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<ClassModeRoute> {
            ClassModeDestination(
                onLeave = back,
                onFinished = { id ->
                    nav.navigate(PostClassRoute(id)) { popUpTo<ClassModeRoute> { inclusive = true } }
                },
                onAttendance = { nav.navigate(AttendanceRoute(it)) },
                onTimer = { nav.navigate(ClassTimerRoute(it)) },
            )
        }
        composable<PostClassRoute> {
            PostClassDestination(onDone = back, onAttendance = { nav.navigate(AttendanceRoute(it)) })
        }
        composable<AttendanceRoute> { AttendanceDestination(onBack = back) }
        composable<ClassTimerRoute> { TimerDestination(onBack = back) }
        composable<StudentEditorRoute> {
            StudentEditorDestination(
                onClose = back,
                onDeleted = { nav.popBackStack<StudentsRoute>(inclusive = false) },
            )
        }
        composable<StudentDetailRoute> {
            StudentDetailDestination(
                onBack = back,
                onEdit = { nav.navigate(StudentEditorRoute(it)) },
                onOpenSession = { nav.navigate(SessionDetailRoute(it)) },
            )
        }
        composable<TechniqueEditorRoute> { TechniqueEditorDestination(onClose = back) }
        composable<SettingsRoute> { SettingsDestination(onBack = back, onPrograms = { nav.navigate(ProgramsRoute) }) }
        composable<ProgramsRoute> { ProgramsDestination(onBack = back) }
    }
}
