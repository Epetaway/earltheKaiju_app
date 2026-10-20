package com.earlthekaiju.coach.screenshots

import com.earlthekaiju.coach.data.db.ClassSessionEntity
import com.earlthekaiju.coach.data.db.LessonPlanEntity
import com.earlthekaiju.coach.data.db.LessonPlanSummary
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.RosterRow
import com.earlthekaiju.coach.data.db.SessionNoteEntity
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.data.db.StudentAttendanceRow
import com.earlthekaiju.coach.data.db.StudentEntity
import com.earlthekaiju.coach.data.db.StudentListItem
import com.earlthekaiju.coach.data.db.TechniqueEntity
import com.earlthekaiju.coach.data.db.TechniqueWithUsage
import com.earlthekaiju.coach.data.db.TimerPresetEntity
import com.earlthekaiju.coach.data.repo.LessonPlanDraft
import com.earlthekaiju.coach.data.repo.SegmentDraft
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Audience
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.model.StudentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Fictional data for screenshots. Every name here is invented; no real student appears in the
 * repository. Dates are fixed so screenshots are reproducible: Tuesday, October 20, 2026, 5:45 PM.
 */
object Fx {
    val now: LocalDateTime = LocalDateTime.of(2026, 10, 20, 17, 45)
    val today: LocalDate = now.toLocalDate()
    private const val T = 1_792_534_000_000L

    val programs = listOf(
        ProgramEntity("p-adults", "Adult Fundamentals", Audience.ADULTS, ProgramColor.EMERALD, createdAt = T, updatedAt = T),
        ProgramEntity("p-all", "Adult All Levels", Audience.ADULTS, ProgramColor.GOLD, sortOrder = 1, createdAt = T, updatedAt = T),
        ProgramEntity("p-kids", "Kids 6–12", Audience.KIDS, ProgramColor.ACCENT, sortOrder = 2, createdAt = T, updatedAt = T),
    )

    fun session(
        id: String,
        title: String,
        date: LocalDate,
        time: LocalTime,
        minutes: Int,
        status: SessionStatus = SessionStatus.SCHEDULED,
        programId: String? = "p-adults",
        lessonPlanId: String? = "l-guard",
        currentSegment: Int = 0,
    ) = ClassSessionEntity(
        id = id, title = title, programId = programId, lessonPlanId = lessonPlanId,
        date = date, startTime = time, durationMinutes = minutes, location = "Main mat", status = status,
        currentSegmentIndex = currentSegment, createdAt = T, updatedAt = T,
    )

    fun summary(
        s: ClassSessionEntity,
        program: String? = "Adult Fundamentals",
        color: ProgramColor? = ProgramColor.EMERALD,
        lesson: String? = "Guard Retention",
        expected: Int = 8,
        present: Int = 0,
    ) = SessionSummary(s, program, color, lesson, expected, present)

    val nextClass = summary(session("s-fund", "Fundamentals", today, LocalTime.of(18, 30), 60))
    val kidsDone = summary(
        session("s-kids", "Kids 6–12", today, LocalTime.of(17, 0), 45, SessionStatus.COMPLETED, "p-kids", "l-kids"),
        "Kids 6–12", ProgramColor.ACCENT, "Safe Falls & Shrimping", expected = 4, present = 4,
    )
    val openMat = summary(
        session("s-open", "Open Mat", today, LocalTime.of(20, 0), 75, programId = "p-all", lessonPlanId = null),
        "Adult All Levels", ProgramColor.GOLD, null, expected = 3,
    )
    val liveClass = summary(
        session("s-fund", "Fundamentals", today, LocalTime.of(18, 30), 60, SessionStatus.IN_PROGRESS, currentSegment = 1),
        present = 6,
    )

    val guardRetentionSegments = listOf(
        Triple("Movement preparation", 10, SegmentKind.WARM_UP),
        Triple("Guard retention concepts", 20, SegmentKind.INSTRUCTION),
        Triple("Partner drilling", 15, SegmentKind.DRILLING),
        Triple("Positional sparring", 15, SegmentKind.POSITIONAL),
    )

    val techniques = listOf(
        TechniqueWithUsage(TechniqueEntity("t1", "Frames and hip escape", "Guard retention", "Frame first, then move the hips.", T, T), 2),
        TechniqueWithUsage(TechniqueEntity("t2", "Knee shield recovery", "Guard retention", "", T, T), 1),
        TechniqueWithUsage(TechniqueEntity("t3", "Inversion to guard", "Guard retention", "Advanced students only.", T, T), 1),
        TechniqueWithUsage(TechniqueEntity("t4", "Scissor sweep", "Closed guard", "", T, T), 1),
        TechniqueWithUsage(TechniqueEntity("t5", "Hip bump sweep", "Closed guard", "Sit up tall, post, bump.", T, T), 1),
        TechniqueWithUsage(TechniqueEntity("t6", "Knee cut pass", "Passing", "", T, T), 0),
        TechniqueWithUsage(TechniqueEntity("t7", "Breakfalls", "Movement", "", T, T), 1),
    )

    val lessonDraft = LessonPlanDraft(
        id = "l-guard",
        title = "Guard Retention",
        programId = "p-adults",
        targetMinutes = 60,
        objective = "Keep the guard in front of you: frames first, then hips, then legs.",
        notes = "Pair new white belts with experienced partners for the positional round.",
        segments = listOf(
            SegmentDraft("g1", "Movement preparation", 10, SegmentKind.WARM_UP, "Shrimps, technical stand-ups, breakfalls.", listOf("t7")),
            SegmentDraft("g2", "Guard retention concepts", 20, SegmentKind.INSTRUCTION, "Frame on the hip and collarbone. Re-guard before the pass settles.", listOf("t1", "t2", "t3")),
            SegmentDraft("g3", "Partner drilling", 15, SegmentKind.DRILLING, "Passer at 50%."),
            SegmentDraft("g4", "Positional sparring", 15, SegmentKind.POSITIONAL, "Start in open guard. Reset on pass or sweep."),
        ),
    )

    val lessons = listOf(
        LessonPlanSummary(LessonPlanEntity("l-guard", "Guard Retention", "p-adults", 60, "Keep the guard in front of you: frames first, then hips, then legs.", createdAt = T, updatedAt = T), "Adult Fundamentals", 4, 60),
        LessonPlanSummary(LessonPlanEntity("l-closed", "Closed Guard Sweeps", "p-all", 75, "Break posture, off-balance, then sweep.", createdAt = T, updatedAt = T), "Adult All Levels", 4, 75),
        LessonPlanSummary(LessonPlanEntity("l-kids", "Safe Falls & Shrimping", "p-kids", 45, "Fall safely and move off the bottom.", createdAt = T, updatedAt = T), "Kids 6–12", 5, 45),
    )

    private fun student(id: String, first: String, last: String, belt: Belt, stripes: Int, minor: Boolean = false, status: StudentStatus = StudentStatus.ACTIVE) =
        StudentEntity(id, first, last, belt, stripes, minor, status, createdAt = T, updatedAt = T)

    val students = listOf(
        StudentListItem(student("st1", "Ana", "Ribeiro", Belt.BLUE, 2), "Adult Fundamentals, Adult All Levels", 9, today.minusDays(1)),
        StudentListItem(student("st2", "Dre", "Coleman", Belt.BROWN, 2), "Adult All Levels", 6, today.minusDays(3)),
        StudentListItem(student("st3", "Eli", "R.", Belt.GREY_WHITE, 1, minor = true), "Kids 6–12", 8, today),
        StudentListItem(student("st4", "Jonah", "Kim", Belt.WHITE, 0), "Adult Fundamentals", 3, today.minusDays(6)),
        StudentListItem(student("st5", "Lena", "Okafor", Belt.WHITE, 2), "Adult Fundamentals", 7, today.minusDays(1)),
        StudentListItem(student("st6", "Marcus", "Hale", Belt.WHITE, 3), "Adult Fundamentals", 8, today.minusDays(1)),
        StudentListItem(student("st7", "Noor", "A.", Belt.YELLOW, 2, minor = true), "Kids 6–12", 8, today),
        StudentListItem(student("st8", "Priya", "Nair", Belt.PURPLE, 1), "Adult All Levels", 5, today.minusDays(3)),
        StudentListItem(student("st9", "Sam", "Whitfield", Belt.WHITE, 1), "Adult Fundamentals", 0, null),
        StudentListItem(student("st10", "Tess", "Morales", Belt.BLUE, 0), "Adult Fundamentals, Adult All Levels", 10, today.minusDays(1)),
    )

    val roster = listOf(
        RosterRow("st1", "Ana", "Ribeiro", Belt.BLUE, 2, false, true, AttendanceStatus.PRESENT),
        RosterRow("st4", "Jonah", "Kim", Belt.WHITE, 0, false, true, AttendanceStatus.ABSENT),
        RosterRow("st5", "Lena", "Okafor", Belt.WHITE, 2, false, true, AttendanceStatus.PRESENT),
        RosterRow("st6", "Marcus", "Hale", Belt.WHITE, 3, false, true, AttendanceStatus.PRESENT),
        RosterRow("st8", "Priya", "Nair", Belt.PURPLE, 1, false, false, AttendanceStatus.PRESENT),
        RosterRow("st9", "Sam", "Whitfield", Belt.WHITE, 1, false, true, null),
        RosterRow("st10", "Tess", "Morales", Belt.BLUE, 0, false, true, AttendanceStatus.PRESENT),
    )

    val history = listOf(
        StudentAttendanceRow("h1", today.minusDays(1), LocalTime.of(18, 30), "Fundamentals", "Adult Fundamentals", AttendanceStatus.PRESENT),
        StudentAttendanceRow("h2", today.minusDays(3), LocalTime.of(10, 0), "All Levels", "Adult All Levels", AttendanceStatus.PRESENT),
        StudentAttendanceRow("h3", today.minusDays(8), LocalTime.of(18, 30), "Fundamentals", "Adult Fundamentals", AttendanceStatus.ABSENT),
        StudentAttendanceRow("h4", today.minusDays(10), LocalTime.of(10, 0), "All Levels", "Adult All Levels", AttendanceStatus.PRESENT),
    )

    val presets = listOf(
        TimerPresetEntity("tp1", "Sparring rounds", 10, 300, 60, 5, 30, 0, T, T),
        TimerPresetEntity("tp2", "Positional", 5, 180, 30, 6, 15, 1, T, T),
        TimerPresetEntity("tp3", "Kids rounds", 10, 120, 60, 4, 15, 2, T, T),
        TimerPresetEntity("tp4", "Drilling switches", 5, 90, 10, 6, 0, 3, T, T),
    )

    val notes = listOf(
        SessionNoteEntity("n1", "s-fund", "Knee shield recovery clicked once they framed on the collarbone first.", T, T),
        SessionNoteEntity("n2", "s-fund", "Sam needs more reps on hip escapes — pair with Lena next week.", T, T),
    )
}
