package com.earlthekaiju.coach.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface ProgramDao {
    @Query("SELECT * FROM program WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM program WHERE id = :id")
    suspend fun get(id: String): ProgramEntity?

    @Query("SELECT COUNT(*) FROM program WHERE deletedAt IS NULL")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(program: ProgramEntity)

    /** Removes a program: tombstone it, drop its enrollments, unassign it from upcoming classes. */
    @Transaction
    suspend fun softDelete(id: String, now: Long) {
        markDeleted(id, now)
        deleteEnrollments(id)
        detachFromScheduledSessions(id, now)
    }

    @Query("UPDATE program SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    @Query("DELETE FROM enrollment WHERE programId = :id")
    suspend fun deleteEnrollments(id: String)

    @Query("UPDATE class_session SET programId = NULL, updatedAt = :now WHERE programId = :id AND status = 'SCHEDULED'")
    suspend fun detachFromScheduledSessions(id: String, now: Long)
}

data class LessonPlanSummary(
    @Embedded val plan: LessonPlanEntity,
    val programName: String?,
    val segmentCount: Int,
    val segmentMinutes: Int,
)

data class SegmentTechniqueRow(
    val segmentId: String,
    val techniqueId: String,
    val name: String,
    val position: Int,
)

@Dao
interface LessonDao {
    @Query(
        """
        SELECT lp.*, p.name AS programName,
            (SELECT COUNT(*) FROM lesson_segment s WHERE s.lessonPlanId = lp.id) AS segmentCount,
            (SELECT COALESCE(SUM(s.minutes), 0) FROM lesson_segment s WHERE s.lessonPlanId = lp.id) AS segmentMinutes
        FROM lesson_plan lp
        LEFT JOIN program p ON p.id = lp.programId AND p.deletedAt IS NULL
        WHERE lp.deletedAt IS NULL AND lp.isTemplate = 1
        ORDER BY lp.updatedAt DESC
        """,
    )
    fun observeTemplateSummaries(): Flow<List<LessonPlanSummary>>

    @Query("SELECT * FROM lesson_plan WHERE id = :id AND deletedAt IS NULL")
    fun observePlan(id: String): Flow<LessonPlanEntity?>

    @Query("SELECT * FROM lesson_plan WHERE id = :id")
    suspend fun getPlan(id: String): LessonPlanEntity?

    @Query("SELECT * FROM lesson_segment WHERE lessonPlanId = :planId ORDER BY position")
    fun observeSegments(planId: String): Flow<List<LessonSegmentEntity>>

    @Query("SELECT * FROM lesson_segment WHERE lessonPlanId = :planId ORDER BY position")
    suspend fun getSegments(planId: String): List<LessonSegmentEntity>

    @Query(
        """
        SELECT st.segmentId, st.techniqueId, t.name, st.position
        FROM segment_technique st
        JOIN technique t ON t.id = st.techniqueId
        JOIN lesson_segment s ON s.id = st.segmentId
        WHERE s.lessonPlanId = :planId AND t.deletedAt IS NULL
        ORDER BY st.position
        """,
    )
    fun observeSegmentTechniques(planId: String): Flow<List<SegmentTechniqueRow>>

    @Query(
        """
        SELECT st.segmentId, st.techniqueId, t.name, st.position
        FROM segment_technique st
        JOIN technique t ON t.id = st.techniqueId
        JOIN lesson_segment s ON s.id = st.segmentId
        WHERE s.lessonPlanId = :planId AND t.deletedAt IS NULL
        ORDER BY st.position
        """,
    )
    suspend fun getSegmentTechniques(planId: String): List<SegmentTechniqueRow>

    @Upsert
    suspend fun upsertPlan(plan: LessonPlanEntity)

    @Query("DELETE FROM lesson_segment WHERE lessonPlanId = :planId")
    suspend fun deleteSegments(planId: String)

    @Insert
    suspend fun insertSegments(segments: List<LessonSegmentEntity>)

    @Insert
    suspend fun insertSegmentTechniques(refs: List<SegmentTechniqueCrossRef>)

    /** Saves a plan and replaces its segments (and their technique links) in one transaction. */
    @Transaction
    suspend fun replacePlan(
        plan: LessonPlanEntity,
        segments: List<LessonSegmentEntity>,
        refs: List<SegmentTechniqueCrossRef>,
    ) {
        upsertPlan(plan)
        deleteSegments(plan.id)
        if (segments.isNotEmpty()) insertSegments(segments)
        if (refs.isNotEmpty()) insertSegmentTechniques(refs)
    }

    @Query("SELECT COUNT(*) FROM class_session WHERE lessonPlanId = :planId AND status = 'SCHEDULED' AND deletedAt IS NULL")
    suspend fun countScheduledUsing(planId: String): Int

    @Transaction
    suspend fun softDeletePlan(planId: String, now: Long) {
        markPlanDeleted(planId, now)
        detachFromScheduledSessions(planId, now)
    }

    @Query("UPDATE lesson_plan SET deletedAt = :now, updatedAt = :now WHERE id = :planId")
    suspend fun markPlanDeleted(planId: String, now: Long)

    @Query("UPDATE class_session SET lessonPlanId = NULL, updatedAt = :now WHERE lessonPlanId = :planId AND status = 'SCHEDULED'")
    suspend fun detachFromScheduledSessions(planId: String, now: Long)
}

data class TechniqueWithUsage(
    @Embedded val technique: TechniqueEntity,
    val usageCount: Int,
)

@Dao
interface TechniqueDao {
    @Query(
        """
        SELECT t.*,
            (SELECT COUNT(*) FROM segment_technique st
                JOIN lesson_segment s ON s.id = st.segmentId
                JOIN lesson_plan lp ON lp.id = s.lessonPlanId
                WHERE st.techniqueId = t.id AND lp.deletedAt IS NULL) AS usageCount
        FROM technique t
        WHERE t.deletedAt IS NULL
        ORDER BY t.category COLLATE NOCASE, t.name COLLATE NOCASE
        """,
    )
    fun observeAll(): Flow<List<TechniqueWithUsage>>

    @Query("SELECT * FROM technique WHERE id = :id")
    suspend fun get(id: String): TechniqueEntity?

    @Upsert
    suspend fun upsert(technique: TechniqueEntity)

    @Query("UPDATE technique SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

data class SessionSummary(
    @Embedded val session: ClassSessionEntity,
    val programName: String?,
    val programColor: ProgramColor?,
    val lessonTitle: String?,
    val expectedCount: Int,
    val presentCount: Int,
)

const val SESSION_SUMMARY_SELECT = """
    SELECT cs.*, p.name AS programName, p.color AS programColor, lp.title AS lessonTitle,
        (SELECT COUNT(*) FROM enrollment e JOIN student st ON st.id = e.studentId
            WHERE e.programId = cs.programId AND st.deletedAt IS NULL AND st.status = 'ACTIVE') AS expectedCount,
        (SELECT COUNT(*) FROM attendance a
            WHERE a.sessionId = cs.id AND a.deletedAt IS NULL AND a.status = 'PRESENT') AS presentCount
    FROM class_session cs
    LEFT JOIN program p ON p.id = cs.programId AND p.deletedAt IS NULL
    LEFT JOIN lesson_plan lp ON lp.id = cs.lessonPlanId AND lp.deletedAt IS NULL
"""

@Dao
interface SessionDao {
    @Query("$SESSION_SUMMARY_SELECT WHERE cs.deletedAt IS NULL AND cs.date BETWEEN :from AND :to ORDER BY cs.date, cs.startTime")
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<SessionSummary>>

    @Query(
        """$SESSION_SUMMARY_SELECT
        WHERE cs.deletedAt IS NULL AND cs.status IN ('SCHEDULED', 'IN_PROGRESS') AND cs.date >= :fromDate
        ORDER BY CASE cs.status WHEN 'IN_PROGRESS' THEN 0 ELSE 1 END, cs.date, cs.startTime
        LIMIT 30""",
    )
    fun observeUpcoming(fromDate: LocalDate): Flow<List<SessionSummary>>

    @Query("$SESSION_SUMMARY_SELECT WHERE cs.id = :id AND cs.deletedAt IS NULL")
    fun observeSummary(id: String): Flow<SessionSummary?>

    @Query("SELECT * FROM class_session WHERE id = :id")
    suspend fun get(id: String): ClassSessionEntity?

    @Query("SELECT * FROM class_session WHERE status = 'IN_PROGRESS' AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeInProgress(): Flow<ClassSessionEntity?>

    @Upsert
    suspend fun upsert(session: ClassSessionEntity)

    @Query("UPDATE class_session SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT * FROM session_segment WHERE sessionId = :sessionId ORDER BY position")
    fun observeSegments(sessionId: String): Flow<List<SessionSegmentEntity>>

    @Query("SELECT * FROM session_segment WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getSegments(sessionId: String): List<SessionSegmentEntity>

    @Insert
    suspend fun insertSegments(segments: List<SessionSegmentEntity>)

    @Upsert
    suspend fun upsertSegment(segment: SessionSegmentEntity)

    @Query("DELETE FROM session_segment WHERE sessionId = :sessionId")
    suspend fun deleteSegments(sessionId: String)

    /** Starts a class: snapshot the lesson into session segments and mark it in progress. */
    @Transaction
    suspend fun start(session: ClassSessionEntity, snapshot: List<SessionSegmentEntity>) {
        deleteSegments(session.id)
        if (snapshot.isNotEmpty()) insertSegments(snapshot)
        upsert(session)
    }

    /**
     * Moves a live class to segment [index] in one transaction: earlier segments end, the target
     * restarts, later ones reset. Atomic so two quick taps can't interleave half-written rows.
     */
    @Transaction
    suspend fun goToSegment(id: String, index: Int, now: Long) {
        val session = get(id) ?: return
        val segments = getSegments(id)
        if (index !in segments.indices) return
        segments.forEach { s ->
            val updated = when {
                s.position < index && s.endedAt == null -> s.copy(endedAt = now, startedAt = s.startedAt ?: now, updatedAt = now)
                s.position == index -> s.copy(startedAt = now, endedAt = null, updatedAt = now)
                s.position > index && (s.startedAt != null || s.endedAt != null) -> s.copy(startedAt = null, endedAt = null, updatedAt = now)
                else -> null
            }
            if (updated != null) upsertSegment(updated)
        }
        upsert(session.copy(currentSegmentIndex = index, currentSegmentStartedAt = now, updatedAt = now))
    }

    @Query("SELECT * FROM session_note WHERE sessionId = :sessionId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeNotes(sessionId: String): Flow<List<SessionNoteEntity>>

    @Upsert
    suspend fun upsertNote(note: SessionNoteEntity)

    @Query("UPDATE session_note SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteNote(id: String, now: Long)
}

data class StudentListItem(
    @Embedded val student: StudentEntity,
    val programNames: String?,
    val recentAttendance: Int,
    val lastAttendedDate: LocalDate?,
)

data class StudentAttendanceRow(
    val sessionId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val title: String,
    val programName: String?,
    val status: AttendanceStatus,
)

@Dao
interface StudentDao {
    @Query(
        """
        SELECT s.*,
            (SELECT GROUP_CONCAT(p.name, ', ') FROM enrollment e JOIN program p ON p.id = e.programId
                WHERE e.studentId = s.id AND p.deletedAt IS NULL) AS programNames,
            (SELECT COUNT(*) FROM attendance a JOIN class_session cs ON cs.id = a.sessionId
                WHERE a.studentId = s.id AND a.deletedAt IS NULL AND a.status = 'PRESENT'
                AND cs.deletedAt IS NULL AND cs.date >= :since) AS recentAttendance,
            (SELECT MAX(cs.date) FROM attendance a JOIN class_session cs ON cs.id = a.sessionId
                WHERE a.studentId = s.id AND a.deletedAt IS NULL AND a.status = 'PRESENT' AND cs.deletedAt IS NULL) AS lastAttendedDate
        FROM student s
        WHERE s.deletedAt IS NULL
        ORDER BY s.status, s.firstName COLLATE NOCASE, s.lastName COLLATE NOCASE
        """,
    )
    fun observeList(since: LocalDate): Flow<List<StudentListItem>>

    @Query("SELECT * FROM student WHERE id = :id AND deletedAt IS NULL")
    fun observe(id: String): Flow<StudentEntity?>

    @Query("SELECT * FROM student WHERE id = :id")
    suspend fun get(id: String): StudentEntity?

    @Query("SELECT programId FROM enrollment WHERE studentId = :studentId")
    fun observeEnrollments(studentId: String): Flow<List<String>>

    @Query("SELECT programId FROM enrollment WHERE studentId = :studentId")
    suspend fun getEnrollments(studentId: String): List<String>

    @Upsert
    suspend fun upsert(student: StudentEntity)

    @Query("DELETE FROM enrollment WHERE studentId = :studentId")
    suspend fun clearEnrollments(studentId: String)

    @Insert
    suspend fun insertEnrollments(enrollments: List<EnrollmentEntity>)

    @Transaction
    suspend fun save(student: StudentEntity, programIds: Collection<String>, now: Long) {
        upsert(student)
        clearEnrollments(student.id)
        if (programIds.isNotEmpty()) {
            insertEnrollments(programIds.map { EnrollmentEntity(studentId = student.id, programId = it, createdAt = now) })
        }
    }

    @Query(
        """
        SELECT cs.id AS sessionId, cs.date, cs.startTime, cs.title, p.name AS programName, a.status
        FROM attendance a
        JOIN class_session cs ON cs.id = a.sessionId
        LEFT JOIN program p ON p.id = cs.programId
        WHERE a.studentId = :studentId AND a.deletedAt IS NULL AND cs.deletedAt IS NULL
        ORDER BY cs.date DESC, cs.startTime DESC
        """,
    )
    fun observeAttendanceHistory(studentId: String): Flow<List<StudentAttendanceRow>>

    /** Permanent removal request: tombstone the student and their attendance marks. */
    @Transaction
    suspend fun softDeleteWithAttendance(studentId: String, now: Long) {
        markStudentDeleted(studentId, now)
        markAttendanceDeleted(studentId, now)
        clearEnrollments(studentId)
    }

    @Query("UPDATE student SET deletedAt = :now, updatedAt = :now, firstName = '', lastName = '', notes = '' WHERE id = :studentId")
    suspend fun markStudentDeleted(studentId: String, now: Long)

    @Query("UPDATE attendance SET deletedAt = :now, updatedAt = :now WHERE studentId = :studentId")
    suspend fun markAttendanceDeleted(studentId: String, now: Long)
}

data class RosterRow(
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val belt: Belt,
    val stripes: Int,
    val isMinor: Boolean,
    val enrolled: Boolean,
    val status: AttendanceStatus?,
)

@Dao
interface AttendanceDao {
    /**
     * Expected students (active and enrolled in the class's program) plus anyone already marked
     * for this session (drop-ins, or students enrolled elsewhere).
     */
    @Query(
        """
        SELECT s.id AS studentId, s.firstName, s.lastName, s.belt, s.stripes, s.isMinor,
            EXISTS(SELECT 1 FROM enrollment e WHERE e.studentId = s.id AND e.programId = :programId) AS enrolled,
            a.status AS status
        FROM student s
        LEFT JOIN attendance a ON a.studentId = s.id AND a.sessionId = :sessionId AND a.deletedAt IS NULL
        WHERE s.deletedAt IS NULL AND (
            (s.status = 'ACTIVE' AND EXISTS(SELECT 1 FROM enrollment e WHERE e.studentId = s.id AND e.programId = :programId))
            OR a.id IS NOT NULL
        )
        ORDER BY s.firstName COLLATE NOCASE, s.lastName COLLATE NOCASE
        """,
    )
    fun observeRoster(sessionId: String, programId: String?): Flow<List<RosterRow>>

    @Query("SELECT * FROM attendance WHERE sessionId = :sessionId AND studentId = :studentId")
    suspend fun find(sessionId: String, studentId: String): AttendanceEntity?

    @Upsert
    suspend fun upsert(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE sessionId = :sessionId AND deletedAt IS NULL")
    suspend fun forSession(sessionId: String): List<AttendanceEntity>
}

@Dao
interface TimerPresetDao {
    @Query("SELECT * FROM timer_preset WHERE deletedAt IS NULL ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<TimerPresetEntity>>

    @Query("SELECT COUNT(*) FROM timer_preset")
    suspend fun countIncludingDeleted(): Int

    @Upsert
    suspend fun upsert(preset: TimerPresetEntity)

    @Insert
    suspend fun insertAll(presets: List<TimerPresetEntity>)

    @Query("UPDATE timer_preset SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}

/** Whole-table reads for the JSON backup export (tombstones included, so a restore is faithful). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM program") suspend fun programs(): List<ProgramEntity>
    @Query("SELECT * FROM lesson_plan") suspend fun lessonPlans(): List<LessonPlanEntity>
    @Query("SELECT * FROM lesson_segment") suspend fun lessonSegments(): List<LessonSegmentEntity>
    @Query("SELECT * FROM technique") suspend fun techniques(): List<TechniqueEntity>
    @Query("SELECT * FROM segment_technique") suspend fun segmentTechniques(): List<SegmentTechniqueCrossRef>
    @Query("SELECT * FROM class_session") suspend fun sessions(): List<ClassSessionEntity>
    @Query("SELECT * FROM session_segment") suspend fun sessionSegments(): List<SessionSegmentEntity>
    @Query("SELECT * FROM session_note") suspend fun sessionNotes(): List<SessionNoteEntity>
    @Query("SELECT * FROM student") suspend fun students(): List<StudentEntity>
    @Query("SELECT * FROM enrollment") suspend fun enrollments(): List<EnrollmentEntity>
    @Query("SELECT * FROM attendance") suspend fun attendance(): List<AttendanceEntity>
    @Query("SELECT * FROM timer_preset") suspend fun timerPresets(): List<TimerPresetEntity>
}
