package com.earlthekaiju.coach.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.earlthekaiju.coach.data.backup.LocalDateIso
import com.earlthekaiju.coach.data.backup.LocalTimeIso
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Audience
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.domain.model.StudentStatus
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

/*
 * Kaiju Coach schema, version 1.
 *
 * Conventions (see docs/05-data-model.md):
 *  - Every id is a random UUID string, created on the device, stable forever. No autoincrement ids,
 *    so records from the phone and the future desktop companion never collide.
 *  - createdAt / updatedAt are epoch milliseconds (UTC). updatedAt changes on every edit.
 *  - Top-level records are soft-deleted: deletedAt is set and the row stays as a tombstone so a
 *    future sync can tell the other device it was removed. Queries filter deletedAt IS NULL.
 *  - Child rows owned by one parent (segments, enrollments, cross-refs) are replaced with the
 *    parent; their parent's updatedAt is bumped so sync treats the parent as one unit.
 *  - Class times are local wall-clock values (date + time), because "Tuesday 6:30pm" must stay
 *    6:30pm across daylight-saving changes.
 */

@Serializable
@Entity(tableName = "program")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val audience: Audience,
    val color: ProgramColor,
    val notes: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A reusable lesson plan (template). Never edited by running a class; see [SessionSegmentEntity]. */
@Serializable
@Entity(
    tableName = "lesson_plan",
    foreignKeys = [
        ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("programId")],
)
data class LessonPlanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val programId: String?,
    /** The length the instructor is aiming for; segments may add up to more or less. */
    val targetMinutes: Int,
    val objective: String = "",
    val notes: String = "",
    /** Saved to the reusable library (true) or a one-off plan for a single class (false). */
    val isTemplate: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(
    tableName = "lesson_segment",
    foreignKeys = [
        ForeignKey(entity = LessonPlanEntity::class, parentColumns = ["id"], childColumns = ["lessonPlanId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("lessonPlanId")],
)
data class LessonSegmentEntity(
    @PrimaryKey val id: String,
    val lessonPlanId: String,
    val position: Int,
    val title: String,
    val minutes: Int,
    val kind: SegmentKind = SegmentKind.OTHER,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "technique")
data class TechniqueEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Free text, e.g. "Guard retention", "Passing", "Takedowns". */
    val category: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
@Entity(
    tableName = "segment_technique",
    primaryKeys = ["segmentId", "techniqueId"],
    foreignKeys = [
        ForeignKey(entity = LessonSegmentEntity::class, parentColumns = ["id"], childColumns = ["segmentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TechniqueEntity::class, parentColumns = ["id"], childColumns = ["techniqueId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("techniqueId")],
)
data class SegmentTechniqueCrossRef(
    val segmentId: String,
    val techniqueId: String,
    val position: Int,
)

/** One scheduled class on a specific date. Separate from the reusable lesson plan it uses. */
@Serializable
@Entity(
    tableName = "class_session",
    foreignKeys = [
        ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = LessonPlanEntity::class, parentColumns = ["id"], childColumns = ["lessonPlanId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("date"), Index("programId"), Index("lessonPlanId"), Index("status")],
)
data class ClassSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val programId: String?,
    /** The template this class follows. Copied into session_segment when the class starts. */
    val lessonPlanId: String?,
    @Serializable(with = LocalDateIso::class) val date: LocalDate,
    @Serializable(with = LocalTimeIso::class) val startTime: LocalTime,
    val durationMinutes: Int,
    val location: String = "",
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    /** Class Mode position, persisted so a restart resumes on the same segment. */
    val currentSegmentIndex: Int = 0,
    val currentSegmentStartedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/**
 * The lesson as it was actually taught: a snapshot of the plan's segments taken when the class
 * starts. Editing the template later never rewrites a past class.
 */
@Serializable
@Entity(
    tableName = "session_segment",
    foreignKeys = [
        ForeignKey(entity = ClassSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId")],
)
data class SessionSegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val position: Int,
    val title: String,
    val minutes: Int,
    val kind: SegmentKind,
    val notes: String,
    /** Technique names at the time of the class, one per line (a snapshot, not a reference). */
    val techniqueNames: String,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * An enrolled student. Deliberately minimal: name, rank, kids/adult flag, a short instructor note.
 * No birth dates, contact details, photos or health information in Sprint 01.
 */
@Serializable
@Entity(tableName = "student", indices = [Index("lastName"), Index("status")])
data class StudentEntity(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val belt: Belt = Belt.WHITE,
    val stripes: Int = 0,
    /** Under 18: shown with a KIDS marker; drives extra care in exports and future sync. */
    val isMinor: Boolean = false,
    val status: StudentStatus = StudentStatus.ACTIVE,
    /** Operational teaching note only (e.g. "working on frames"). */
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** A student belongs to programs; expected attendance for a class comes from its program. */
@Serializable
@Entity(
    tableName = "enrollment",
    primaryKeys = ["studentId", "programId"],
    foreignKeys = [
        ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProgramEntity::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("programId")],
)
data class EnrollmentEntity(
    val studentId: String,
    val programId: String,
    val createdAt: Long,
)

/** One student's attendance at one class session. Unique per (session, student). */
@Serializable
@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(entity = ClassSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["sessionId", "studentId"], unique = true), Index("studentId")],
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val studentId: String,
    val status: AttendanceStatus,
    val markedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    /** Set when a mark is undone back to "unmarked"; kept as a tombstone for sync. */
    val deletedAt: Long? = null,
)

@Serializable
@Entity(tableName = "timer_preset")
data class TimerPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val prepSeconds: Int,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    /** Seconds before the end of a work round when the warning cue plays (0 = off). */
    val warningSeconds: Int,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Notes written during or after a class, attached to that class session. */
@Serializable
@Entity(
    tableName = "session_note",
    foreignKeys = [
        ForeignKey(entity = ClassSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId")],
)
data class SessionNoteEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
