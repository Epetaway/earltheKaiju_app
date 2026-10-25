package com.earlthekaiju.coach.data.sample

import com.earlthekaiju.coach.data.repo.AttendanceRepository
import com.earlthekaiju.coach.data.repo.LessonPlanDraft
import com.earlthekaiju.coach.data.repo.LessonRepository
import com.earlthekaiju.coach.data.repo.ProgramDraft
import com.earlthekaiju.coach.data.repo.ProgramRepository
import com.earlthekaiju.coach.data.repo.SegmentDraft
import com.earlthekaiju.coach.data.repo.SessionDraft
import com.earlthekaiju.coach.data.repo.SessionRepository
import com.earlthekaiju.coach.data.repo.StudentDraft
import com.earlthekaiju.coach.data.repo.StudentRepository
import com.earlthekaiju.coach.data.repo.TechniqueDraft
import com.earlthekaiju.coach.data.repo.TechniqueRepository
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Audience
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.time.AppClock
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Debug-only demo content for trying the app and for emulator screenshots.
 * Every student here is fictional. Real rosters are never committed to the repository.
 */
class SampleData(
    private val clock: AppClock,
    private val programs: ProgramRepository,
    private val lessons: LessonRepository,
    private val techniques: TechniqueRepository,
    private val sessions: SessionRepository,
    private val students: StudentRepository,
    private val attendance: AttendanceRepository,
) {
    suspend fun load() {
        val adults = programs.save(ProgramDraft(name = "Adult Fundamentals", audience = Audience.ADULTS, color = ProgramColor.EMERALD))
        val allLevels = programs.save(ProgramDraft(name = "Adult All Levels", audience = Audience.ADULTS, color = ProgramColor.GOLD))
        val kids = programs.save(ProgramDraft(name = "Kids 6–12", audience = Audience.KIDS, color = ProgramColor.ACCENT))

        val t = listOf(
            TechniqueDraft(name = "Frames and hip escape", category = "Guard retention"),
            TechniqueDraft(name = "Knee shield recovery", category = "Guard retention"),
            TechniqueDraft(name = "Inversion to guard", category = "Guard retention"),
            TechniqueDraft(name = "Scissor sweep", category = "Closed guard"),
            TechniqueDraft(name = "Hip bump sweep", category = "Closed guard"),
            TechniqueDraft(name = "Toreando pass", category = "Passing"),
            TechniqueDraft(name = "Knee cut pass", category = "Passing"),
            TechniqueDraft(name = "Technical stand-up", category = "Self-defense"),
            TechniqueDraft(name = "Shrimping drill", category = "Movement"),
            TechniqueDraft(name = "Breakfalls", category = "Movement"),
        ).map { techniques.save(it) }

        val guardRetention = lessons.save(
            LessonPlanDraft(
                title = "Guard Retention",
                programId = adults,
                targetMinutes = 60,
                objective = "Keep the guard in front of you: frames first, then hips, then legs.",
                segments = listOf(
                    SegmentDraft(title = "Movement preparation", minutes = 10, kind = SegmentKind.WARM_UP, techniqueIds = listOf(t[8], t[9])),
                    SegmentDraft(title = "Guard retention concepts", minutes = 20, kind = SegmentKind.INSTRUCTION, notes = "Frame on the hip and collarbone. Re-guard before the pass settles.", techniqueIds = listOf(t[0], t[1], t[2])),
                    SegmentDraft(title = "Partner drilling", minutes = 15, kind = SegmentKind.DRILLING, notes = "Passer at 50%, guard player works the frame first."),
                    SegmentDraft(title = "Positional sparring", minutes = 15, kind = SegmentKind.POSITIONAL, notes = "Start in open guard. Reset on pass or sweep."),
                ),
            ),
        )
        val closedGuard = lessons.save(
            LessonPlanDraft(
                title = "Closed Guard Sweeps",
                programId = allLevels,
                targetMinutes = 75,
                objective = "Break posture, off-balance, then sweep.",
                segments = listOf(
                    SegmentDraft(title = "Warm-up", minutes = 10, kind = SegmentKind.WARM_UP),
                    SegmentDraft(title = "Scissor and hip bump", minutes = 20, kind = SegmentKind.INSTRUCTION, techniqueIds = listOf(t[3], t[4])),
                    SegmentDraft(title = "Chain drilling", minutes = 15, kind = SegmentKind.DRILLING),
                    SegmentDraft(title = "Rolls", minutes = 30, kind = SegmentKind.SPARRING),
                ),
            ),
        )
        val kidsPlan = lessons.save(
            LessonPlanDraft(
                title = "Safe Falls & Shrimping",
                programId = kids,
                targetMinutes = 45,
                objective = "Fall safely and move off the bottom.",
                segments = listOf(
                    SegmentDraft(title = "Animal walks", minutes = 8, kind = SegmentKind.WARM_UP),
                    SegmentDraft(title = "Breakfalls", minutes = 10, kind = SegmentKind.INSTRUCTION, techniqueIds = listOf(t[9])),
                    SegmentDraft(title = "Shrimping races", minutes = 12, kind = SegmentKind.DRILLING, techniqueIds = listOf(t[8])),
                    SegmentDraft(title = "King of the mat", minutes = 10, kind = SegmentKind.POSITIONAL),
                    SegmentDraft(title = "Bow out and line up", minutes = 5, kind = SegmentKind.REVIEW),
                ),
            ),
        )

        // Fictional roster.
        val adultRoster = listOf(
            StudentDraft(firstName = "Ana", lastName = "Ribeiro", belt = Belt.BLUE, stripes = 2, programIds = setOf(adults, allLevels)),
            StudentDraft(firstName = "Marcus", lastName = "Hale", belt = Belt.WHITE, stripes = 3, programIds = setOf(adults)),
            StudentDraft(firstName = "Priya", lastName = "Nair", belt = Belt.PURPLE, stripes = 1, programIds = setOf(allLevels)),
            StudentDraft(firstName = "Jonah", lastName = "Kim", belt = Belt.WHITE, stripes = 0, programIds = setOf(adults)),
            StudentDraft(firstName = "Tess", lastName = "Morales", belt = Belt.BLUE, stripes = 0, programIds = setOf(adults, allLevels)),
            StudentDraft(firstName = "Dre", lastName = "Coleman", belt = Belt.BROWN, stripes = 2, programIds = setOf(allLevels)),
            StudentDraft(firstName = "Sam", lastName = "Whitfield", belt = Belt.WHITE, stripes = 1, programIds = setOf(adults)),
            StudentDraft(firstName = "Lena", lastName = "Okafor", belt = Belt.WHITE, stripes = 2, programIds = setOf(adults)),
        ).map { students.save(it) }
        val kidsRoster = listOf(
            StudentDraft(firstName = "Eli", lastName = "R.", belt = Belt.GREY_WHITE, stripes = 1, isMinor = true, programIds = setOf(kids)),
            StudentDraft(firstName = "Noor", lastName = "A.", belt = Belt.YELLOW, stripes = 2, isMinor = true, programIds = setOf(kids)),
            StudentDraft(firstName = "Mateo", lastName = "S.", belt = Belt.WHITE, stripes = 3, isMinor = true, programIds = setOf(kids)),
            StudentDraft(firstName = "Zoe", lastName = "L.", belt = Belt.GREY, stripes = 0, isMinor = true, programIds = setOf(kids)),
        ).map { students.save(it) }

        // This week: Mon/Wed adults, Tue/Thu kids, Sat all levels.
        val today = clock.today()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun at(day: DayOfWeek) = monday.plusDays((day.value - 1).toLong())
        val plan = listOf(
            Triple(DayOfWeek.MONDAY, adults, guardRetention),
            Triple(DayOfWeek.TUESDAY, kids, kidsPlan),
            Triple(DayOfWeek.WEDNESDAY, adults, guardRetention),
            Triple(DayOfWeek.THURSDAY, kids, kidsPlan),
            Triple(DayOfWeek.SATURDAY, allLevels, closedGuard),
        )
        for ((day, program, lesson) in plan) {
            val date = at(day)
            val isKids = program == kids
            val id = sessions.save(
                SessionDraft(
                    title = when (program) { kids -> "Kids 6–12"; allLevels -> "All Levels"; else -> "Fundamentals" },
                    programId = program,
                    lessonPlanId = lesson,
                    date = date,
                    startTime = if (isKids) LocalTime.of(17, 0) else if (day == DayOfWeek.SATURDAY) LocalTime.of(10, 0) else LocalTime.of(18, 30),
                    durationMinutes = if (isKids) 45 else if (day == DayOfWeek.SATURDAY) 75 else 60,
                    location = "Main mat",
                ),
            )
            if (date.isBefore(today)) {
                sessions.start(id)
                val roster = if (isKids) kidsRoster else adultRoster
                roster.forEachIndexed { i, sid ->
                    attendance.mark(id, sid, if ((i + day.value) % 4 == 0) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT)
                }
                sessions.finish(id)
            }
        }
        // One extra class today if today has none in the plan, so Today always shows a next class.
        if (plan.none { at(it.first) == today }) {
            sessions.save(
                SessionDraft(
                    title = "Open Mat",
                    programId = allLevels,
                    lessonPlanId = closedGuard,
                    date = today,
                    startTime = LocalTime.of(19, 0),
                    durationMinutes = 75,
                    location = "Main mat",
                ),
            )
        }
    }
}
