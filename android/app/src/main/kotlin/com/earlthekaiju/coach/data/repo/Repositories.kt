package com.earlthekaiju.coach.data.repo

import com.earlthekaiju.coach.data.db.AttendanceEntity
import com.earlthekaiju.coach.data.db.ClassSessionEntity
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.db.LessonPlanEntity
import com.earlthekaiju.coach.data.db.LessonPlanSummary
import com.earlthekaiju.coach.data.db.LessonSegmentEntity
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.RosterRow
import com.earlthekaiju.coach.data.db.SegmentTechniqueCrossRef
import com.earlthekaiju.coach.data.db.SessionNoteEntity
import com.earlthekaiju.coach.data.db.SessionSegmentEntity
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.data.db.StudentAttendanceRow
import com.earlthekaiju.coach.data.db.StudentEntity
import com.earlthekaiju.coach.data.db.StudentListItem
import com.earlthekaiju.coach.data.db.TechniqueEntity
import com.earlthekaiju.coach.data.db.TechniqueWithUsage
import com.earlthekaiju.coach.data.db.TimerPresetEntity
import com.earlthekaiju.coach.data.prefs.SettingsRepository
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.time.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class ProgramRepository(db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.programDao()

    fun observeAll(): Flow<List<ProgramEntity>> = dao.observeAll()

    suspend fun get(id: String): ProgramEntity? = dao.get(id)

    suspend fun save(draft: ProgramDraft): String {
        val now = clock.now()
        val existing = draft.id?.let { dao.get(it) }
        val id = existing?.id ?: draft.id ?: newId()
        dao.upsert(
            ProgramEntity(
                id = id,
                name = draft.name.trim(),
                audience = draft.audience,
                color = draft.color,
                notes = draft.notes.trim(),
                sortOrder = existing?.sortOrder ?: dao.count(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun delete(id: String) = dao.softDelete(id, clock.now())
}

class LessonRepository(private val db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.lessonDao()

    fun observeTemplates(): Flow<List<LessonPlanSummary>> = dao.observeTemplateSummaries()

    fun observeDetail(planId: String): Flow<LessonPlanDetail?> =
        combine(dao.observePlan(planId), dao.observeSegments(planId), dao.observeSegmentTechniques(planId)) { plan, segments, techniques ->
            plan?.let {
                LessonPlanDetail(
                    plan = it,
                    segments = segments.map { s -> SegmentDetail(s, techniques.filter { t -> t.segmentId == s.id }) },
                )
            }
        }

    suspend fun loadDraft(planId: String): LessonPlanDraft? {
        val plan = dao.getPlan(planId)?.takeIf { it.deletedAt == null } ?: return null
        val techniques = dao.getSegmentTechniques(planId)
        return LessonPlanDraft(
            id = plan.id,
            title = plan.title,
            programId = plan.programId,
            targetMinutes = plan.targetMinutes,
            objective = plan.objective,
            notes = plan.notes,
            isTemplate = plan.isTemplate,
            segments = dao.getSegments(planId).map { s ->
                SegmentDraft(
                    id = s.id,
                    title = s.title,
                    minutes = s.minutes,
                    kind = s.kind,
                    notes = s.notes,
                    techniqueIds = techniques.filter { it.segmentId == s.id }.map { it.techniqueId },
                )
            },
        )
    }

    /** Saves the plan and its ordered segments atomically. Returns the plan id. */
    suspend fun save(draft: LessonPlanDraft): String {
        val now = clock.now()
        val existing = draft.id?.let { dao.getPlan(it) }
        val id = existing?.id ?: draft.id ?: newId()
        val previousSegments = existing?.let { dao.getSegments(it.id) }.orEmpty().associateBy { it.id }
        val plan = LessonPlanEntity(
            id = id,
            title = draft.title.trim(),
            programId = draft.programId,
            targetMinutes = draft.targetMinutes,
            objective = draft.objective.trim(),
            notes = draft.notes.trim(),
            isTemplate = draft.isTemplate,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val segments = draft.segments.mapIndexed { index, s ->
            LessonSegmentEntity(
                id = s.id,
                lessonPlanId = id,
                position = index,
                title = s.title.trim().ifEmpty { "Segment ${index + 1}" },
                minutes = s.minutes,
                kind = s.kind,
                notes = s.notes.trim(),
                createdAt = previousSegments[s.id]?.createdAt ?: now,
                updatedAt = now,
            )
        }
        val refs = draft.segments.flatMap { s ->
            s.techniqueIds.distinct().mapIndexed { i, techniqueId -> SegmentTechniqueCrossRef(s.id, techniqueId, i) }
        }
        dao.replacePlan(plan, segments, refs)
        return id
    }

    suspend fun duplicate(planId: String): String? {
        val draft = loadDraft(planId) ?: return null
        return save(
            draft.copy(
                id = null,
                title = "${draft.title} (copy)",
                segments = draft.segments.map { it.copy(id = newId()) },
            ),
        )
    }

    suspend fun countScheduledUsing(planId: String): Int = dao.countScheduledUsing(planId)

    suspend fun delete(planId: String) = dao.softDeletePlan(planId, clock.now())
}

class TechniqueRepository(db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.techniqueDao()

    fun observeAll(): Flow<List<TechniqueWithUsage>> = dao.observeAll()

    suspend fun get(id: String): TechniqueEntity? = dao.get(id)?.takeIf { it.deletedAt == null }

    suspend fun save(draft: TechniqueDraft): String {
        val now = clock.now()
        val existing = draft.id?.let { dao.get(it) }
        val id = existing?.id ?: draft.id ?: newId()
        dao.upsert(
            TechniqueEntity(
                id = id,
                name = draft.name.trim(),
                category = draft.category.trim(),
                notes = draft.notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun delete(id: String) = dao.softDelete(id, clock.now())
}

class SessionRepository(private val db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.sessionDao()
    private val lessonDao = db.lessonDao()

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<SessionSummary>> = dao.observeBetween(from, to)
    fun observeUpcoming(fromDate: LocalDate): Flow<List<SessionSummary>> = dao.observeUpcoming(fromDate)
    fun observeSummary(id: String): Flow<SessionSummary?> = dao.observeSummary(id)
    fun observeInProgress(): Flow<ClassSessionEntity?> = dao.observeInProgress()
    fun observeSegments(sessionId: String): Flow<List<SessionSegmentEntity>> = dao.observeSegments(sessionId)
    fun observeNotes(sessionId: String): Flow<List<SessionNoteEntity>> = dao.observeNotes(sessionId)

    suspend fun get(id: String): ClassSessionEntity? = dao.get(id)?.takeIf { it.deletedAt == null }

    suspend fun save(draft: SessionDraft): String {
        val now = clock.now()
        val existing = draft.id?.let { dao.get(it) }
        val id = existing?.id ?: draft.id ?: newId()
        val base = existing ?: ClassSessionEntity(
            id = id, title = "", programId = null, lessonPlanId = null,
            date = draft.date, startTime = draft.startTime, durationMinutes = draft.durationMinutes,
            createdAt = now, updatedAt = now,
        )
        dao.upsert(
            base.copy(
                title = draft.title.trim(),
                programId = draft.programId,
                lessonPlanId = draft.lessonPlanId,
                date = draft.date,
                startTime = draft.startTime,
                durationMinutes = draft.durationMinutes,
                location = draft.location.trim(),
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun setStatus(id: String, status: SessionStatus) {
        val s = dao.get(id) ?: return
        dao.upsert(s.copy(status = status, updatedAt = clock.now()))
    }

    suspend fun delete(id: String) = dao.softDelete(id, clock.now())

    /**
     * Starts (or resumes) a class. On first start the lesson plan is copied into session segments,
     * so the class keeps exactly what was taught even if the plan changes later.
     */
    suspend fun start(id: String) {
        val session = dao.get(id) ?: return
        if (session.status == SessionStatus.IN_PROGRESS) return
        val now = clock.now()
        val planSegments = session.lessonPlanId?.let { lessonDao.getSegments(it) }.orEmpty()
        val techniques = session.lessonPlanId?.let { lessonDao.getSegmentTechniques(it) }.orEmpty()
        val snapshot = if (planSegments.isEmpty()) {
            listOf(
                SessionSegmentEntity(
                    id = newId(), sessionId = id, position = 0,
                    title = session.title.ifBlank { "Class" }, minutes = session.durationMinutes,
                    kind = SegmentKind.OTHER, notes = "", techniqueNames = "",
                    startedAt = now, createdAt = now, updatedAt = now,
                ),
            )
        } else {
            planSegments.mapIndexed { index, s ->
                SessionSegmentEntity(
                    id = newId(), sessionId = id, position = index,
                    title = s.title, minutes = s.minutes, kind = s.kind, notes = s.notes,
                    techniqueNames = techniques.filter { it.segmentId == s.id }.joinToString("\n") { it.name },
                    startedAt = if (index == 0) now else null,
                    createdAt = now, updatedAt = now,
                )
            }
        }
        dao.start(
            session.copy(
                status = SessionStatus.IN_PROGRESS,
                startedAt = now,
                endedAt = null,
                currentSegmentIndex = 0,
                currentSegmentStartedAt = now,
                updatedAt = now,
            ),
            snapshot,
        )
    }

    /** Moves Class Mode to [index], closing earlier segments and opening the new one. */
    suspend fun goToSegment(id: String, index: Int) {
        val session = dao.get(id) ?: return
        val segments = dao.getSegments(id)
        if (index !in segments.indices) return
        val now = clock.now()
        segments.forEach { s ->
            val updated = when {
                s.position < index && s.endedAt == null -> s.copy(endedAt = now, startedAt = s.startedAt ?: now, updatedAt = now)
                s.position == index -> s.copy(startedAt = now, endedAt = null, updatedAt = now)
                s.position > index && (s.startedAt != null || s.endedAt != null) -> s.copy(startedAt = null, endedAt = null, updatedAt = now)
                else -> null
            }
            if (updated != null) dao.upsertSegment(updated)
        }
        dao.upsert(session.copy(currentSegmentIndex = index, currentSegmentStartedAt = now, updatedAt = now))
    }

    suspend fun finish(id: String) {
        val session = dao.get(id) ?: return
        val now = clock.now()
        dao.getSegments(id).filter { it.position <= session.currentSegmentIndex && it.endedAt == null }.forEach {
            dao.upsertSegment(it.copy(endedAt = now, startedAt = it.startedAt ?: now, updatedAt = now))
        }
        dao.upsert(session.copy(status = SessionStatus.COMPLETED, endedAt = now, updatedAt = now))
    }

    suspend fun addNote(sessionId: String, body: String): String? {
        val text = body.trim()
        if (text.isEmpty()) return null
        val now = clock.now()
        val id = newId()
        dao.upsertNote(SessionNoteEntity(id = id, sessionId = sessionId, body = text, createdAt = now, updatedAt = now))
        return id
    }

    suspend fun deleteNote(noteId: String) = dao.softDeleteNote(noteId, clock.now())
}

class StudentRepository(db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.studentDao()

    fun observeList(since: LocalDate): Flow<List<StudentListItem>> = dao.observeList(since)
    fun observe(id: String): Flow<StudentEntity?> = dao.observe(id)
    fun observeEnrollments(id: String): Flow<List<String>> = dao.observeEnrollments(id)
    fun observeHistory(id: String): Flow<List<StudentAttendanceRow>> = dao.observeAttendanceHistory(id)

    suspend fun loadDraft(id: String): StudentDraft? {
        val s = dao.get(id)?.takeIf { it.deletedAt == null } ?: return null
        return StudentDraft(
            id = s.id, firstName = s.firstName, lastName = s.lastName, belt = s.belt, stripes = s.stripes,
            isMinor = s.isMinor, status = s.status, notes = s.notes, programIds = dao.getEnrollments(id).toSet(),
        )
    }

    suspend fun save(draft: StudentDraft): String {
        val now = clock.now()
        val existing = draft.id?.let { dao.get(it) }
        val id = existing?.id ?: draft.id ?: newId()
        dao.save(
            StudentEntity(
                id = id,
                firstName = draft.firstName.trim(),
                lastName = draft.lastName.trim(),
                belt = draft.belt,
                stripes = draft.stripes.coerceIn(0, draft.belt.maxStripes),
                isMinor = draft.isMinor,
                status = draft.status,
                notes = draft.notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
            draft.programIds,
            now,
        )
        return id
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        val s = dao.get(id) ?: return
        val status = if (archived) StudentStatus.ARCHIVED else StudentStatus.ACTIVE
        dao.upsert(s.copy(status = status, updatedAt = clock.now()))
    }

    /** Removes the student's name, notes and attendance marks; a nameless tombstone remains for sync. */
    suspend fun deletePermanently(id: String) = dao.softDeleteWithAttendance(id, clock.now())
}

class AttendanceRepository(db: KaijuDatabase, private val clock: AppClock) {
    private val dao = db.attendanceDao()

    fun observeRoster(sessionId: String, programId: String?): Flow<List<RosterRow>> = dao.observeRoster(sessionId, programId)

    /**
     * Sets one student's mark for a session; null clears it (undo back to unmarked).
     * Returns the previous mark so the caller can offer Undo.
     */
    suspend fun mark(sessionId: String, studentId: String, status: AttendanceStatus?): AttendanceStatus? {
        val now = clock.now()
        val existing = dao.find(sessionId, studentId)
        val previous = existing?.takeIf { it.deletedAt == null }?.status
        when {
            existing == null && status == null -> Unit
            existing == null -> dao.upsert(
                AttendanceEntity(
                    id = newId(), sessionId = sessionId, studentId = studentId, status = status!!,
                    markedAt = now, createdAt = now, updatedAt = now,
                ),
            )
            status == null -> dao.upsert(existing.copy(deletedAt = now, updatedAt = now))
            else -> dao.upsert(existing.copy(status = status, markedAt = now, deletedAt = null, updatedAt = now))
        }
        return previous
    }

    /** Marks several students at once (e.g. "everyone present"). Returns their previous marks. */
    suspend fun markAll(sessionId: String, studentIds: List<String>, status: AttendanceStatus): Map<String, AttendanceStatus?> =
        studentIds.associateWith { mark(sessionId, it, status) }

    suspend fun restore(sessionId: String, previous: Map<String, AttendanceStatus?>) {
        previous.forEach { (studentId, status) -> mark(sessionId, studentId, status) }
    }
}

class TimerPresetRepository(
    db: KaijuDatabase,
    private val clock: AppClock,
    private val settings: SettingsRepository,
) {
    private val dao = db.timerPresetDao()

    fun observeAll(): Flow<List<TimerPresetEntity>> = dao.observeAll()

    suspend fun save(draft: TimerPresetDraft): String {
        val now = clock.now()
        val id = draft.id ?: newId()
        dao.upsert(
            TimerPresetEntity(
                id = id,
                name = draft.name.trim().ifEmpty { "Custom" },
                prepSeconds = draft.prepSeconds,
                workSeconds = draft.workSeconds,
                restSeconds = draft.restSeconds,
                rounds = draft.rounds,
                warningSeconds = draft.warningSeconds,
                sortOrder = 100,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun delete(id: String) = dao.softDelete(id, clock.now())

    /**
     * First run only: a few common BJJ round formats so the timer is useful immediately.
     * They are ordinary presets; edit or delete them freely (deleted ones don't come back).
     */
    suspend fun seedDefaultsIfNeeded() {
        if (settings.presetsSeeded() || dao.countIncludingDeleted() > 0) {
            settings.markPresetsSeeded()
            return
        }
        val now = clock.now()
        dao.insertAll(
            listOf(
                TimerPresetEntity(newId(), "Sparring rounds", 10, 300, 60, 5, 30, 0, now, now),
                TimerPresetEntity(newId(), "Positional", 5, 180, 30, 6, 15, 1, now, now),
                TimerPresetEntity(newId(), "Kids rounds", 10, 120, 60, 4, 15, 2, now, now),
                TimerPresetEntity(newId(), "Drilling switches", 5, 90, 10, 6, 0, 3, now, now),
            ),
        )
        settings.markPresetsSeeded()
    }
}
