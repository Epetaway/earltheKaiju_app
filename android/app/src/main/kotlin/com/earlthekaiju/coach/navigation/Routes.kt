package com.earlthekaiju.coach.navigation

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Ids are passed, never whole records.

// Top-level tabs (each its own graph so its back stack is saved and restored).
@Serializable object TodayGraph
@Serializable object TodayRoute
@Serializable object PlannerGraph
@Serializable data class PlannerRoute(val showLessons: Boolean = false)
@Serializable object TimerGraph
@Serializable object TimerRoute
@Serializable object StudentsGraph
@Serializable object StudentsRoute
@Serializable object LibraryGraph
@Serializable object LibraryRoute

// Secondary screens.
@Serializable data class SessionDetailRoute(val sessionId: String)
@Serializable data class SessionEditorRoute(
    val sessionId: String? = null,
    /** ISO date to prefill when scheduling a new class. */
    val date: String? = null,
    val lessonPlanId: String? = null,
)
@Serializable data class LessonEditorRoute(val lessonPlanId: String? = null)
@Serializable data class ClassModeRoute(val sessionId: String)
@Serializable data class PostClassRoute(val sessionId: String)
@Serializable data class AttendanceRoute(val sessionId: String)
@Serializable data class ClassTimerRoute(val sessionId: String? = null)
@Serializable data class StudentEditorRoute(val studentId: String? = null)
@Serializable data class StudentDetailRoute(val studentId: String)
@Serializable data class TechniqueEditorRoute(val techniqueId: String? = null)
@Serializable object SettingsRoute
@Serializable object ProgramsRoute
