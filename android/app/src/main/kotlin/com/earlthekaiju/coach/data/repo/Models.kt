package com.earlthekaiju.coach.data.repo

import com.earlthekaiju.coach.data.db.LessonPlanEntity
import com.earlthekaiju.coach.data.db.LessonSegmentEntity
import com.earlthekaiju.coach.data.db.SegmentTechniqueRow
import com.earlthekaiju.coach.domain.model.Audience
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.ProgramColor
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.domain.time.newId
import java.time.LocalDate
import java.time.LocalTime

/*
 * Editing models ("drafts") used by the editors. A draft with id == null is new.
 * Repositories turn drafts into entities, stamp timestamps and keep ids stable.
 */

data class ProgramDraft(
    val id: String? = null,
    val name: String = "",
    val audience: Audience = Audience.ADULTS,
    val color: ProgramColor = ProgramColor.EMERALD,
    val notes: String = "",
)

data class SegmentDraft(
    /** Stable id, generated once when the segment is added in the builder. */
    val id: String = newId(),
    val title: String = "",
    val minutes: Int = 10,
    val kind: SegmentKind = SegmentKind.OTHER,
    val notes: String = "",
    val techniqueIds: List<String> = emptyList(),
)

data class LessonPlanDraft(
    val id: String? = null,
    val title: String = "",
    val programId: String? = null,
    val targetMinutes: Int = 60,
    val objective: String = "",
    val notes: String = "",
    val isTemplate: Boolean = true,
    val segments: List<SegmentDraft> = emptyList(),
) {
    val segmentMinutes: Int get() = segments.sumOf { it.minutes }
}

data class SegmentDetail(val segment: LessonSegmentEntity, val techniques: List<SegmentTechniqueRow>)

data class LessonPlanDetail(val plan: LessonPlanEntity, val segments: List<SegmentDetail>) {
    val segmentMinutes: Int get() = segments.sumOf { it.segment.minutes }
}

data class SessionDraft(
    val id: String? = null,
    val title: String = "",
    val programId: String? = null,
    val lessonPlanId: String? = null,
    val date: LocalDate,
    val startTime: LocalTime = LocalTime.of(18, 30),
    val durationMinutes: Int = 60,
    val location: String = "",
)

data class StudentDraft(
    val id: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val belt: Belt = Belt.WHITE,
    val stripes: Int = 0,
    val isMinor: Boolean = false,
    val status: StudentStatus = StudentStatus.ACTIVE,
    val notes: String = "",
    val programIds: Set<String> = emptySet(),
)

data class TechniqueDraft(
    val id: String? = null,
    val name: String = "",
    val category: String = "",
    val notes: String = "",
)

data class TimerPresetDraft(
    val id: String? = null,
    val name: String = "",
    val prepSeconds: Int = 10,
    val workSeconds: Int = 300,
    val restSeconds: Int = 60,
    val rounds: Int = 5,
    val warningSeconds: Int = 30,
)
