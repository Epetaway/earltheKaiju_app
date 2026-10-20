package com.earlthekaiju.coach.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ClassCard
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuCard
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KeystoneRule
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.LiveDot
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentState
import com.earlthekaiju.coach.designsystem.component.SessionStatusBadge
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.programColorValue
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import com.earlthekaiju.coach.ui.displayTitle
import com.earlthekaiju.coach.ui.hasEnded
import com.earlthekaiju.coach.ui.toCardModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class SegmentPreview(val title: String, val minutes: Int, val kindLabel: String?)

data class TodayUiState(
    val loading: Boolean = true,
    val now: LocalDateTime = LocalDateTime.now(),
    /** The class in progress, or else the next scheduled one. */
    val featured: SessionSummary? = null,
    val featuredObjective: String? = null,
    val featuredSegments: List<SegmentPreview> = emptyList(),
    val otherToday: List<SessionSummary> = emptyList(),
    val todayCount: Int = 0,
    val lessonPlanCount: Int = 0,
) {
    val date: LocalDate get() = now.toLocalDate()
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val c: AppContainer) : ViewModel() {
    private val minuteTicker = flow {
        while (true) {
            emit(c.clock.localDateTime())
            delay(60_000L - c.clock.now() % 60_000L + 50L)
        }
    }

    val state: StateFlow<TodayUiState> = minuteTicker.flatMapLatest { now ->
        val today = now.toLocalDate()
        combine(
            c.sessions.observeUpcoming(today),
            c.sessions.observeBetween(today, today),
            c.lessons.observeTemplates(),
        ) { upcoming, todays, templates -> Triple(upcoming, todays, templates.size) }
            .flatMapLatest { (upcoming, todays, lessonCount) ->
                val live = upcoming.firstOrNull { it.session.status == SessionStatus.IN_PROGRESS }
                val next = upcoming.firstOrNull { it.session.status == SessionStatus.SCHEDULED && !it.hasEnded(now) }
                val featured = live ?: next
                val details = when {
                    featured == null -> flowOf(null to emptyList())
                    featured.session.status == SessionStatus.IN_PROGRESS ->
                        c.sessions.observeSegments(featured.session.id).map { segs ->
                            null to segs.map { SegmentPreview(it.title, it.minutes, it.kind.label) }
                        }
                    featured.session.lessonPlanId != null ->
                        c.lessons.observeDetail(featured.session.lessonPlanId).map { d ->
                            d?.plan?.objective?.takeIf { it.isNotBlank() } to
                                d?.segments.orEmpty().map { SegmentPreview(it.segment.title, it.segment.minutes, it.segment.kind.label) }
                        }
                    else -> flowOf(null to emptyList())
                }
                details.map { (objective, segments) ->
                    TodayUiState(
                        loading = false,
                        now = now,
                        featured = featured,
                        featuredObjective = objective,
                        featuredSegments = segments,
                        otherToday = todays.filter { it.session.id != featured?.session?.id },
                        todayCount = todays.count { it.session.status != SessionStatus.CANCELED },
                        lessonPlanCount = lessonCount,
                    )
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
}

data class TodayActions(
    val onStartClass: (String) -> Unit = {},
    val onOpenSession: (String) -> Unit = {},
    val onAttendance: (String) -> Unit = {},
    val onScheduleClass: () -> Unit = {},
    val onNewLesson: () -> Unit = {},
    val onOpenLessons: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun TodayDestination(actions: TodayActions) {
    val vm = kaijuViewModel { c, _ -> TodayViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    TodayScreen(state, actions)
}

@Composable
fun TodayScreen(state: TodayUiState, actions: TodayActions, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            TodayHeader(state, actions.onOpenSettings, Modifier.readableWidth())
        }
        if (state.loading) {
            item { LoadingState("Loading today", Modifier.readableWidth()) }
            return@LazyColumn
        }
        val featured = state.featured
        if (featured == null) {
            item(key = "empty") {
                EmptyState(
                    eyebrow = "Nothing scheduled",
                    title = "No class on the schedule",
                    body = if (state.lessonPlanCount > 0) {
                        "Schedule your next class, or start from one of your ${Format.count(state.lessonPlanCount, "saved lesson plan")}."
                    } else {
                        "Schedule your next class. You can attach a lesson plan now or later."
                    },
                    primary = StateAction("Schedule class", actions.onScheduleClass),
                    secondary = StateAction(if (state.lessonPlanCount > 0) "Saved lesson plans" else "Build a lesson plan", if (state.lessonPlanCount > 0) actions.onOpenLessons else actions.onNewLesson),
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.l),
                )
            }
        } else {
            item(key = "next-header") {
                SectionHeader(
                    title = if (featured.session.status == SessionStatus.IN_PROGRESS) "In progress" else "Next class",
                    index = 1,
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.l),
                )
            }
            item(key = "featured") {
                FeaturedClassCard(state, actions, Modifier.readableWidth().padding(top = KaijuSpace.xs))
            }
        }
        if (state.otherToday.isNotEmpty()) {
            item(key = "others-header") {
                SectionHeader(
                    title = if (featured == null) "Today" else "Also today",
                    index = if (featured == null) 1 else 2,
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.xl),
                )
            }
            items(state.otherToday, key = { it.session.id }) { s ->
                ClassCard(
                    s.toCardModel(LocalUse24Hour.current),
                    onClick = { actions.onOpenSession(s.session.id) },
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.xs),
                )
            }
        }
        item(key = "plan-ahead") {
            val index = 1 + (if (featured != null) 1 else 0) + (if (state.otherToday.isNotEmpty()) 1 else 0)
            Column(Modifier.readableWidth().padding(top = KaijuSpace.xl)) {
                SectionHeader(title = "Plan ahead", index = index)
                Spacer(Modifier.height(KaijuSpace.s))
                Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
                    KaijuButton("Schedule class", actions.onScheduleClass, Modifier.weight(1f), variant = ButtonVariant.Secondary, icon = KaijuIcons.Add)
                    KaijuButton("New lesson", actions.onNewLesson, Modifier.weight(1f), variant = ButtonVariant.Secondary, icon = KaijuIcons.Lessons)
                }
                if (state.lessonPlanCount > 0) {
                    Spacer(Modifier.height(KaijuSpace.xs))
                    KaijuButton(
                        "Lesson library · ${state.lessonPlanCount}",
                        actions.onOpenLessons,
                        variant = ButtonVariant.Ghost,
                        size = ButtonSize.Compact,
                    )
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
    }
}

@Composable
private fun TodayHeader(state: TodayUiState, onOpenSettings: () -> Unit, modifier: Modifier) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    Column(modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(Format.dayEyebrow(state.date), Modifier.weight(1f))
            KaijuIconButton(KaijuIcons.Settings, contentDescription = "Settings", onClick = onOpenSettings, tint = colors.inkMuted)
        }
        Text(
            "TODAY",
            style = Kaiju.type.displayXL,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(KaijuSpace.s))
        KeystoneRule(width = 136.dp)
        Spacer(Modifier.height(KaijuSpace.m))
        val summary = when {
            state.loading -> ""
            state.todayCount == 0 -> "No classes today."
            else -> {
                val next = state.featured?.takeIf { it.session.date == state.date && it.session.status == SessionStatus.SCHEDULED }
                Format.count(state.todayCount, "class", "classes") + " today" +
                    (next?.let { " · next at ${Format.time(it.session.startTime, use24h)}" } ?: "")
            }
        }
        if (summary.isNotEmpty()) Text(summary, style = Kaiju.type.bodyL, color = colors.inkMuted)
    }
}

@Composable
private fun FeaturedClassCard(state: TodayUiState, actions: TodayActions, modifier: Modifier) {
    val s = state.featured ?: return
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    val live = s.session.status == SessionStatus.IN_PROGRESS
    KaijuCard(
        modifier = modifier,
        shape = KaijuShapes.CardFeature,
        accentEdge = if (live) colors.live else programColorValue(s.programColor),
        contentPadding = KaijuSpace.m,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(s.programName ?: "No program", Modifier.weight(1f))
            SessionStatusBadge(s.session.status)
        }
        Spacer(Modifier.height(KaijuSpace.s))
        Text(
            (s.lessonTitle ?: s.displayTitle).uppercase(),
            style = Kaiju.type.displayM,
            color = colors.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (s.lessonTitle != null && s.session.title.isNotBlank() && s.session.title != s.lessonTitle) {
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text(s.session.title, style = Kaiju.type.label, color = colors.inkMuted)
        }
        Spacer(Modifier.height(KaijuSpace.m))
        // Editorial data row: three facts separated by hairlines, not three rounded stat cards.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Fact(
                label = if (s.session.date == state.date) "Starts" else Format.relativeDay(s.session.date, state.date),
                value = Format.timeNumerals(s.session.startTime, use24h),
                suffix = Format.meridiem(s.session.startTime, use24h),
                modifier = Modifier.weight(1.2f),
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            Fact("Length", s.session.durationMinutes.toString(), "min", Modifier.weight(1f).padding(start = KaijuSpace.s))
            Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            Fact(
                label = if (live) "Present" else "Expected",
                value = (if (live) s.presentCount else s.expectedCount).toString(),
                suffix = if (live) "of ${s.expectedCount}" else "",
                modifier = Modifier.weight(1f).padding(start = KaijuSpace.s),
            )
        }
        val startsIn = Duration.between(state.now, s.session.date.atTime(s.session.startTime)).toMinutes()
        if (!live && s.session.date == state.date && startsIn in 0..180) {
            Spacer(Modifier.height(KaijuSpace.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(color = colors.gold, size = 8.dp, pulse = false)
                Spacer(Modifier.width(KaijuSpace.xs))
                Text(
                    if (startsIn == 0L) "Starting now" else "Starts in ${Format.minutes(startsIn.toInt())}",
                    style = Kaiju.type.label,
                    color = colors.gold,
                )
            }
        }
        state.featuredObjective?.let {
            Spacer(Modifier.height(KaijuSpace.m))
            Eyebrow("Objective", color = colors.inkMuted)
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text(it, style = Kaiju.type.bodyL, color = colors.ink)
        }
        if (state.featuredSegments.isNotEmpty()) {
            Spacer(Modifier.height(KaijuSpace.m))
            Eyebrow("Segments · ${Format.minutes(state.featuredSegments.sumOf { it.minutes })}", color = colors.inkMuted)
            Spacer(Modifier.height(KaijuSpace.xxs))
            state.featuredSegments.forEachIndexed { i, seg ->
                val segState = when {
                    !live -> SegmentState.Upcoming
                    i < s.session.currentSegmentIndex -> SegmentState.Done
                    i == s.session.currentSegmentIndex -> SegmentState.Current
                    else -> SegmentState.Upcoming
                }
                LessonSegmentRow(index = i + 1, title = seg.title, minutes = seg.minutes, compact = true, state = segState)
                if (i < state.featuredSegments.lastIndex) HairlineDivider(Modifier.fillMaxWidth())
            }
        } else if (s.session.lessonPlanId == null && !live) {
            Spacer(Modifier.height(KaijuSpace.m))
            Text("No lesson plan attached yet.", style = Kaiju.type.bodyS, color = colors.inkMuted)
        }
        Spacer(Modifier.height(KaijuSpace.l))
        KaijuButton(
            text = if (live) "Resume class" else "Start class",
            onClick = { actions.onStartClass(s.session.id) },
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Hero,
            icon = KaijuIcons.Play,
        )
        Spacer(Modifier.height(KaijuSpace.s))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            KaijuButton("Attendance", { actions.onAttendance(s.session.id) }, Modifier.weight(1f), variant = ButtonVariant.Secondary, icon = KaijuIcons.Attendance)
            KaijuButton("Details", { actions.onOpenSession(s.session.id) }, Modifier.weight(1f), variant = ButtonVariant.Secondary)
        }
    }
}

@Composable
private fun Fact(label: String, value: String, suffix: String, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    Column(modifier.semantics(mergeDescendants = true) { }) {
        Eyebrow(label, color = colors.inkMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = Kaiju.type.numeralL, color = colors.ink, maxLines = 1)
            if (suffix.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Text(suffix.uppercase(), style = Kaiju.type.eyebrow, color = colors.inkMuted, modifier = Modifier.padding(bottom = 6.dp), maxLines = 1)
            }
        }
    }
}
