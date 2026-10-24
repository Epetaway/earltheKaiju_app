package com.earlthekaiju.coach.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.data.db.KaijuDatabase
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
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.time.AppClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private class FakeClock(var nowMillis: Long = 1_792_521_000_000L) : AppClock {
    override fun now(): Long = nowMillis
    override fun elapsedRealtime(): Long = nowMillis
    override fun zone(): ZoneId = ZoneId.of("America/New_York")
    fun advance(minutes: Long) { nowMillis += minutes * 60_000 }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class RepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var dbFile: File
    private lateinit var db: KaijuDatabase
    private val clock = FakeClock()

    private fun open(): KaijuDatabase =
        Room.databaseBuilder(context, KaijuDatabase::class.java, dbFile.absolutePath).allowMainThreadQueries().build()

    @Before fun setUp() {
        dbFile = File(context.cacheDir, "test-${System.nanoTime()}.db")
        db = open()
    }

    @After fun tearDown() {
        db.close()
        context.deleteDatabase(dbFile.absolutePath)
    }

    private fun lessons() = LessonRepository(db, clock)
    private fun sessions() = SessionRepository(db, clock)

    @Test
    fun `lesson plan saves segments in order with techniques and reloads them`() = runTest {
        val tech = TechniqueRepository(db, clock)
        val frames = tech.save(TechniqueDraft(name = "Frames", category = "Guard retention"))
        val id = lessons().save(
            LessonPlanDraft(
                title = "Guard Retention",
                targetMinutes = 60,
                segments = listOf(
                    SegmentDraft(title = "Movement preparation", minutes = 10, kind = SegmentKind.WARM_UP),
                    SegmentDraft(title = "Guard retention concepts", minutes = 20, kind = SegmentKind.INSTRUCTION, techniqueIds = listOf(frames)),
                    SegmentDraft(title = "Partner drilling", minutes = 15, kind = SegmentKind.DRILLING),
                    SegmentDraft(title = "Positional sparring", minutes = 15, kind = SegmentKind.POSITIONAL),
                ),
            ),
        )
        val draft = lessons().loadDraft(id)!!
        assertEquals(listOf("Movement preparation", "Guard retention concepts", "Partner drilling", "Positional sparring"), draft.segments.map { it.title })
        assertEquals(60, draft.segmentMinutes)
        assertEquals(listOf(frames), draft.segments[1].techniqueIds)

        // Reorder and save again: ids stay stable, order follows the list.
        val reordered = draft.copy(segments = listOf(draft.segments[1], draft.segments[0]) + draft.segments.drop(2))
        lessons().save(reordered)
        val again = lessons().loadDraft(id)!!
        assertEquals(reordered.segments.map { it.id }, again.segments.map { it.id })
    }

    @Test
    fun `starting a class snapshots the plan so later template edits do not rewrite history`() = runTest {
        val planId = lessons().save(
            LessonPlanDraft(title = "Closed Guard", segments = listOf(SegmentDraft(title = "Scissor sweep", minutes = 20))),
        )
        val sessionId = sessions().save(SessionDraft(title = "Fundamentals", lessonPlanId = planId, date = LocalDate.of(2026, 10, 20)))
        sessions().start(sessionId)
        sessions().finish(sessionId)

        val plan = lessons().loadDraft(planId)!!
        lessons().save(plan.copy(segments = listOf(plan.segments[0].copy(title = "Hip bump sweep", minutes = 25))))

        val taught = sessions().observeSegments(sessionId).first()
        assertEquals(listOf("Scissor sweep"), taught.map { it.title })
        assertEquals(20, taught.single().minutes)
        assertEquals(SessionStatus.COMPLETED, sessions().get(sessionId)!!.status)
    }

    @Test
    fun `class without a plan starts with one segment for its full length`() = runTest {
        val id = sessions().save(SessionDraft(title = "Open mat", date = LocalDate.of(2026, 10, 21), durationMinutes = 90))
        sessions().start(id)
        val segs = sessions().observeSegments(id).first()
        assertEquals(1, segs.size)
        assertEquals(90, segs.single().minutes)
    }

    @Test
    fun `class mode position survives reopening the database`() = runTest {
        val planId = lessons().save(
            LessonPlanDraft(title = "Plan", segments = listOf(SegmentDraft(title = "A", minutes = 5), SegmentDraft(title = "B", minutes = 5))),
        )
        val id = sessions().save(SessionDraft(lessonPlanId = planId, date = LocalDate.of(2026, 10, 20)))
        sessions().start(id)
        clock.advance(6)
        sessions().goToSegment(id, 1)

        db.close()
        db = open()
        val s = sessions().get(id)!!
        assertEquals(SessionStatus.IN_PROGRESS, s.status)
        assertEquals(1, s.currentSegmentIndex)
        assertNotNull(sessions().observeSegments(id).first()[0].endedAt)
    }

    @Test
    fun `stepping back in class mode reopens that segment and resets later ones`() = runTest {
        val planId = lessons().save(
            LessonPlanDraft(title = "Plan", segments = listOf("A", "B", "C", "D").map { SegmentDraft(title = it, minutes = 5) }),
        )
        val id = sessions().save(SessionDraft(lessonPlanId = planId, date = LocalDate.of(2026, 10, 20)))
        sessions().start(id)
        for (i in 1..3) { clock.advance(5); sessions().goToSegment(id, i) }
        clock.advance(1)
        sessions().goToSegment(id, 1)
        sessions().goToSegment(id, 9) // out of range: ignored

        val segs = sessions().observeSegments(id).first()
        assertEquals(1, sessions().get(id)!!.currentSegmentIndex)
        assertNotNull(segs[0].endedAt)
        assertNotNull(segs[1].startedAt)
        assertEquals(null, segs[1].endedAt)
        assertEquals(listOf(null, null), segs.drop(2).map { it.startedAt })
        assertEquals(listOf(null, null), segs.drop(2).map { it.endedAt })
    }

    @Test
    fun `attendance marks, undo and restore after restart`() = runTest {
        val programs = ProgramRepository(db, clock)
        val kids = programs.save(ProgramDraft(name = "Kids", audience = Audience.KIDS))
        val students = StudentRepository(db, clock)
        val a = students.save(StudentDraft(firstName = "Eli", belt = Belt.GREY_WHITE, isMinor = true, programIds = setOf(kids)))
        val b = students.save(StudentDraft(firstName = "Noor", belt = Belt.YELLOW, isMinor = true, programIds = setOf(kids)))
        val sessionId = sessions().save(SessionDraft(programId = kids, date = LocalDate.of(2026, 10, 20), startTime = LocalTime.of(17, 0)))
        val attendance = AttendanceRepository(db, clock)

        // Expected roster comes from the program.
        assertEquals(setOf(a, b), attendance.observeRoster(sessionId, kids).first().map { it.studentId }.toSet())

        val previous = attendance.mark(sessionId, a, AttendanceStatus.PRESENT)
        assertNull(previous)
        attendance.mark(sessionId, b, AttendanceStatus.ABSENT)
        // Undo the absent mark: back to unmarked.
        attendance.mark(sessionId, b, null)

        db.close()
        db = open()
        val roster = AttendanceRepository(db, clock).observeRoster(sessionId, kids).first().associateBy { it.studentId }
        assertEquals(AttendanceStatus.PRESENT, roster[a]!!.status)
        assertNull(roster[b]!!.status)
        assertEquals(1, sessions().observeSummary(sessionId).first()!!.presentCount)
    }

    @Test
    fun `drop-in appears on the roster once marked`() = runTest {
        val programs = ProgramRepository(db, clock)
        val adults = programs.save(ProgramDraft(name = "Adults"))
        val students = StudentRepository(db, clock)
        val visitor = students.save(StudentDraft(firstName = "Visitor"))
        val sessionId = sessions().save(SessionDraft(programId = adults, date = LocalDate.of(2026, 10, 20)))
        val attendance = AttendanceRepository(db, clock)
        assertTrue(attendance.observeRoster(sessionId, adults).first().isEmpty())
        attendance.mark(sessionId, visitor, AttendanceStatus.PRESENT)
        val row = attendance.observeRoster(sessionId, adults).first().single()
        assertEquals(visitor, row.studentId)
        assertEquals(false, row.enrolled)
    }

    @Test
    fun `deleting a plan unassigns it from scheduled classes but keeps taught ones`() = runTest {
        val planId = lessons().save(LessonPlanDraft(title = "Plan", segments = listOf(SegmentDraft(title = "A", minutes = 10))))
        val upcoming = sessions().save(SessionDraft(lessonPlanId = planId, date = LocalDate.of(2026, 10, 22)))
        val past = sessions().save(SessionDraft(lessonPlanId = planId, date = LocalDate.of(2026, 10, 19)))
        sessions().start(past)
        sessions().finish(past)

        assertEquals(1, lessons().countScheduledUsing(planId))
        lessons().delete(planId)

        assertNull(sessions().get(upcoming)!!.lessonPlanId)
        assertEquals(planId, sessions().get(past)!!.lessonPlanId)
        assertEquals(1, sessions().observeSegments(past).first().size)
        assertTrue(lessons().observeTemplates().first().isEmpty())
    }

    @Test
    fun `deleting a student permanently clears their name and attendance`() = runTest {
        val students = StudentRepository(db, clock)
        val id = students.save(StudentDraft(firstName = "Sam", lastName = "Whitfield", notes = "Working on frames"))
        val sessionId = sessions().save(SessionDraft(date = LocalDate.of(2026, 10, 20)))
        AttendanceRepository(db, clock).mark(sessionId, id, AttendanceStatus.PRESENT)

        students.deletePermanently(id)

        val row = db.backupDao().students().single { it.id == id }
        assertEquals("", row.firstName)
        assertEquals("", row.notes)
        assertNotNull(row.deletedAt)
        assertTrue(db.backupDao().attendance().all { it.deletedAt != null })
        assertTrue(students.observeList(LocalDate.of(2026, 9, 1)).first().isEmpty())
    }

    @Test
    fun `every record gets a stable uuid and sync timestamps`() = runTest {
        val id = ProgramRepository(db, clock).save(ProgramDraft(name = "Adults"))
        val created = db.programDao().get(id)!!
        assertEquals(36, id.length)
        assertEquals(created.createdAt, created.updatedAt)
        clock.advance(5)
        ProgramRepository(db, clock).save(ProgramDraft(id = id, name = "Adult Fundamentals"))
        val updated = db.programDao().get(id)!!
        assertEquals(created.createdAt, updated.createdAt)
        assertTrue(updated.updatedAt > created.updatedAt)
    }
}
