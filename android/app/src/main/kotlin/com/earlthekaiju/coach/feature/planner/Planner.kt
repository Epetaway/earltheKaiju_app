package com.earlthekaiju.coach.feature.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.LessonPlanSummary
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ClassCard
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuCard
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.LiveDot
import com.earlthekaiju.coach.designsystem.component.ScreenHeader
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentedControl
import com.earlthekaiju.coach.designsystem.component.StateAction
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
import com.earlthekaiju.coach.ui.toCardModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class PlannerUiState(
    val today: LocalDate,
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val showLessons: Boolean = false,
    val weekSessions: List<SessionSummary> = emptyList(),
    val lessons: List<LessonPlanSummary> = emptyList(),
    val loading: Boolean = true,
) {
    val selectedSessions: List<SessionSummary> get() = weekSessions.filter { it.session.date == selectedDate }
    val days: List<LocalDate> get() = (0L..6L).map { weekStart.plusDays(it) }
}

fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val today = c.clock.today()
    private val selected = MutableStateFlow(today)
    private val lessonsMode = MutableStateFlow(handle.get<Boolean>("showLessons") ?: false)

    val state: StateFlow<PlannerUiState> = combine(selected, lessonsMode) { d, l -> d to l }
        .flatMapLatest { (date, lessons) ->
            val start = weekStartOf(date)
            combine(c.sessions.observeBetween(start, start.plusDays(6)), c.lessons.observeTemplates()) { sessions, plans ->
                PlannerUiState(
                    today = today,
                    weekStart = start,
                    selectedDate = date,
                    showLessons = lessons,
                    weekSessions = sessions,
                    lessons = plans,
                    loading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState(today, weekStartOf(today), today))

    fun select(date: LocalDate) { selected.value = date }
    fun shiftWeek(weeks: Long) { selected.value = selected.value.plusWeeks(weeks) }
    fun goToToday() { selected.value = today }
    fun setShowLessons(show: Boolean) { lessonsMode.value = show }
}

data class PlannerActions(
    val onSelectDate: (LocalDate) -> Unit = {},
    val onShiftWeek: (Long) -> Unit = {},
    val onToday: () -> Unit = {},
    val onShowLessons: (Boolean) -> Unit = {},
    val onOpenSession: (String) -> Unit = {},
    val onSchedule: (LocalDate) -> Unit = {},
    val onOpenLesson: (String) -> Unit = {},
    val onNewLesson: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun PlannerDestination(navigate: PlannerActions) {
    val vm = kaijuViewModel { c, h -> PlannerViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    PlannerScreen(
        state,
        navigate.copy(
            onSelectDate = vm::select,
            onShiftWeek = vm::shiftWeek,
            onToday = vm::goToToday,
            onShowLessons = vm::setShowLessons,
        ),
    )
}

@Composable
fun PlannerScreen(state: PlannerUiState, actions: PlannerActions, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Planner",
                eyebrow = "Week ${Format.weekNumber(state.weekStart)} · ${Format.weekRange(state.weekStart)}",
                modifier = Modifier.readableWidth().windowInsetsPadding(WindowInsets.statusBars),
                trailing = { KaijuIconButton(KaijuIcons.Settings, "Settings", actions.onOpenSettings, tint = colors.inkMuted) },
            )
        }
        item(key = "mode") {
            SegmentedControl(
                options = listOf("Schedule", "Lessons"),
                selectedIndex = if (state.showLessons) 1 else 0,
                onSelect = { actions.onShowLessons(it == 1) },
                modifier = Modifier.readableWidth().padding(top = KaijuSpace.s, bottom = KaijuSpace.m),
            )
        }
        if (!state.showLessons) {
            item(key = "weeknav") { WeekNavigator(state, actions, Modifier.readableWidth()) }
            item(key = "strip") { WeekStrip(state, actions.onSelectDate, Modifier.readableWidth().padding(top = KaijuSpace.s)) }
            item(key = "day-header") {
                SectionHeader(
                    title = "${Format.dayName(state.selectedDate)} ${state.selectedDate.dayOfMonth}",
                    index = state.selectedDate.dayOfWeek.value,
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.l),
                    action = {
                        KaijuButton("Add", { actions.onSchedule(state.selectedDate) }, variant = ButtonVariant.Ghost, size = ButtonSize.Compact, icon = KaijuIcons.Add)
                    },
                )
            }
            if (state.selectedSessions.isEmpty()) {
                item(key = "day-empty") {
                    InlineNote(
                        if (state.selectedDate.isBefore(state.today)) "No classes were scheduled on ${Format.dayName(state.selectedDate)}."
                        else "Nothing scheduled on ${Format.dayName(state.selectedDate)} yet.",
                        Modifier.readableWidth(),
                    )
                }
            } else {
                items(state.selectedSessions, key = { it.session.id }) { s ->
                    ClassCard(s.toCardModel(use24h), { actions.onOpenSession(s.session.id) }, Modifier.readableWidth().padding(top = KaijuSpace.xs))
                }
            }
            item(key = "schedule") {
                KaijuButton(
                    "Schedule class",
                    { actions.onSchedule(state.selectedDate) },
                    Modifier.readableWidth().padding(top = KaijuSpace.l),
                    icon = KaijuIcons.Add,
                )
            }
            item(key = "legend") { StatusLegend(Modifier.readableWidth().padding(top = KaijuSpace.l, bottom = KaijuSpace.xl)) }
        } else {
            item(key = "new-lesson") {
                KaijuButton("New lesson plan", actions.onNewLesson, Modifier.readableWidth(), icon = KaijuIcons.Add)
            }
            if (state.lessons.isEmpty()) {
                item(key = "lessons-empty") {
                    EmptyState(
                        eyebrow = "Lesson library",
                        title = "No lesson plans yet",
                        body = "Build a plan once — warm-up, instruction, drilling, sparring — and reuse it for any class.",
                        primary = StateAction("Build a lesson plan", actions.onNewLesson),
                        modifier = Modifier.readableWidth().padding(top = KaijuSpace.l),
                    )
                }
            } else {
                item(key = "lessons-header") {
                    SectionHeader("Lesson library · ${state.lessons.size}", Modifier.readableWidth().padding(top = KaijuSpace.l), index = 1)
                }
                items(state.lessons, key = { it.plan.id }) { lesson ->
                    LessonPlanCard(lesson, { actions.onOpenLesson(lesson.plan.id) }, Modifier.readableWidth().padding(top = KaijuSpace.xs))
                }
                item { Spacer(Modifier.height(KaijuSpace.xl)) }
            }
        }
    }
}

@Composable
private fun WeekNavigator(state: PlannerUiState, actions: PlannerActions, modifier: Modifier) {
    val colors = Kaiju.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KaijuIconButton(KaijuIcons.ChevronLeft, "Previous week", { actions.onShiftWeek(-1) }, filled = true)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Format.weekRange(state.weekStart).uppercase(), style = Kaiju.type.title, color = colors.ink)
            Text(
                Format.count(state.weekSessions.count { it.session.status != SessionStatus.CANCELED }, "class", "classes") + " this week",
                style = Kaiju.type.caption,
                color = colors.inkMuted,
            )
        }
        KaijuIconButton(KaijuIcons.ChevronRight, "Next week", { actions.onShiftWeek(1) }, filled = true)
        if (weekStartOf(state.today) != state.weekStart || state.selectedDate != state.today) {
            Spacer(Modifier.width(KaijuSpace.xs))
            KaijuButton("Today", actions.onToday, variant = ButtonVariant.Secondary, size = ButtonSize.Compact)
        }
    }
}

/** Seven-day grid: weekday, large date, and one diamond per class (gold when one is live). */
@Composable
private fun WeekStrip(state: PlannerUiState, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val colors = Kaiju.colors
    Row(modifier.fillMaxWidth().border(1.dp, colors.line, KaijuShapes.Small).selectableGroup()) {
        state.days.forEach { day ->
            val sessions = state.weekSessions.filter { it.session.date == day && it.session.status != SessionStatus.CANCELED }
            val selected = day == state.selectedDate
            val isToday = day == state.today
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 84.dp)
                    .background(if (selected) colors.surfaceInset else colors.background)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(day) })
                    .semantics {
                        contentDescription = "${Format.longDate(day)}${if (isToday) ", today" else ""}, " +
                            Format.count(sessions.size, "class", "classes")
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) colors.accent else colors.background))
                Spacer(Modifier.height(KaijuSpace.xs))
                Text(Format.dayLetter(day), style = Kaiju.type.eyebrow, color = if (isToday) colors.gold else colors.inkMuted)
                Text(
                    day.dayOfMonth.toString(),
                    style = Kaiju.type.numeralM,
                    color = if (selected) colors.ink else if (day.isBefore(state.today)) colors.inkMuted else colors.ink,
                    textAlign = TextAlign.Center,
                )
                if (isToday) Box(Modifier.width(14.dp).height(2.dp).background(colors.gold))
                Spacer(Modifier.height(KaijuSpace.xxs))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    sessions.take(3).forEach { s ->
                        LiveDot(
                            color = when (s.session.status) {
                                SessionStatus.IN_PROGRESS -> colors.live
                                SessionStatus.COMPLETED -> colors.inkMuted
                                else -> colors.accent
                            },
                            size = 7.dp,
                            pulse = false,
                        )
                    }
                }
                Spacer(Modifier.height(KaijuSpace.xs))
            }
        }
    }
}

@Composable
private fun StatusLegend(modifier: Modifier) {
    val colors = Kaiju.colors
    Column(modifier) {
        Eyebrow("Key", color = colors.inkMuted)
        Spacer(Modifier.height(KaijuSpace.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.m)) {
            LegendItem(colors.accent, "Scheduled")
            LegendItem(colors.live, "Live")
            LegendItem(colors.inkMuted, "Done")
        }
        Text("Canceled classes are struck through and not counted.", style = Kaiju.type.caption, color = colors.inkMuted, modifier = Modifier.padding(top = KaijuSpace.xxs))
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LiveDot(color = color, size = 8.dp, pulse = false)
        Spacer(Modifier.width(6.dp))
        Text(label, style = Kaiju.type.caption, color = Kaiju.colors.inkMuted)
    }
}

@Composable
fun LessonPlanCard(lesson: LessonPlanSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    KaijuCard(modifier = modifier, onClick = onClick, onClickLabel = "Open lesson plan") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(lesson.programName ?: "Any program", Modifier.weight(1f))
            Text("${lesson.segmentMinutes} / ${lesson.plan.targetMinutes} MIN", style = Kaiju.type.eyebrow, color = colors.inkMuted)
        }
        Spacer(Modifier.height(KaijuSpace.xxs))
        Text(lesson.plan.title.uppercase(), style = Kaiju.type.title, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (lesson.plan.objective.isNotBlank()) {
            Text(lesson.plan.objective, style = Kaiju.type.bodyS, color = colors.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 18.dp, height = 2.dp).background(colors.gold))
            Spacer(Modifier.width(KaijuSpace.xs))
            Text(Format.count(lesson.segmentCount, "segment"), style = Kaiju.type.caption, color = colors.inkMuted)
        }
    }
}
