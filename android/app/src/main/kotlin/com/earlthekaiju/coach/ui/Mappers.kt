package com.earlthekaiju.coach.ui

import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.designsystem.component.ClassCardModel
import java.time.LocalDateTime

fun SessionSummary.toCardModel(use24h: Boolean): ClassCardModel = ClassCardModel(
    id = session.id,
    timeLabel = Format.time(session.startTime, use24h),
    durationLabel = Format.minutes(session.durationMinutes),
    title = session.title.ifBlank { lessonTitle ?: programName ?: "Class" },
    programName = programName,
    programColor = programColor,
    lessonTitle = lessonTitle,
    status = session.status,
    expectedCount = expectedCount,
)

/** Display title for a class: its own title, else the lesson, else the program. */
val SessionSummary.displayTitle: String
    get() = session.title.ifBlank { lessonTitle ?: programName ?: "Class" }

/** True once the class's scheduled end time has passed. */
fun SessionSummary.hasEnded(now: LocalDateTime): Boolean {
    val end = LocalDateTime.of(session.date, session.startTime).plusMinutes(session.durationMinutes.toLong())
    return end.isBefore(now)
}
