package com.earlthekaiju.coach.feature.planner

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.SessionNoteEntity
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.designsystem.component.ButtonPair
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ErrorState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentState
import com.earlthekaiju.coach.designsystem.component.SessionStatusBadge
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.navigation.SessionDetailRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import com.earlthekaiju.coach.ui.displayTitle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailSegment(val title: String, val minutes: Int, val kindLabel: String, val techniques: List<String>, val notes: String, val done: Boolean)

data class SessionDetailUiState(
    val loading: Boolean = true,
    val summary: SessionSummary? = null,
    val segments: List<DetailSegment> = emptyList(),
    val objective: String? = null,
    val notes: List<SessionNoteEntity> = emptyList(),
    val missing: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val sessionId = handle.toRoute<SessionDetailRoute>().sessionId

    val state: StateFlow<SessionDetailUiState> = c.sessions.observeSummary(sessionId).flatMapLatest { summary ->
        if (summary == null) return@flatMapLatest flowOf(SessionDetailUiState(loading = false, missing = true))
        val started = summary.session.status == SessionStatus.IN_PROGRESS || summary.session.status == SessionStatus.COMPLETED
        val segments = if (started) {
            c.sessions.observeSegments(sessionId).map { list ->
                null to list.map { DetailSegment(it.title, it.minutes, it.kind.label, it.techniqueNames.lines().filter(String::isNotBlank), it.notes, it.endedAt != null) }
            }
        } else if (summary.session.lessonPlanId != null) {
            c.lessons.observeDetail(summary.session.lessonPlanId).map { d ->
                d?.plan?.objective?.takeIf { it.isNotBlank() } to d?.segments.orEmpty().map {
                    DetailSegment(it.segment.title, it.segment.minutes, it.segment.kind.label, it.techniques.map { t -> t.name }, it.segment.notes, false)
                }
            }
        } else {
            flowOf(null to emptyList())
        }
        combine(segments, c.sessions.observeNotes(sessionId)) { (objective, segs), notes ->
            SessionDetailUiState(loading = false, summary = summary, segments = segs, objective = objective, notes = notes)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun addNote(text: String) = viewModelScope.launch { c.sessions.addNote(sessionId, text) }
}

data class SessionDetailActions(
    val onBack: () -> Unit = {},
    val onStart: (String) -> Unit = {},
    val onAttendance: (String) -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onOpenLesson: (String) -> Unit = {},
    val onAddNote: (String) -> Unit = {},
)

@Composable
fun SessionDetailDestination(actions: SessionDetailActions) {
    val vm = kaijuViewModel { c, h -> SessionDetailViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    SessionDetailScreen(state, actions.copy(onAddNote = { vm.addNote(it) }))
}

@Composable
fun SessionDetailScreen(state: SessionDetailUiState, actions: SessionDetailActions) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    var noteSheet by remember { mutableStateOf(false) }
    val s = state.summary
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = s?.displayTitle ?: "Class",
            eyebrow = s?.let { "${Format.dayEyebrow(it.session.date)} · ${Format.time(it.session.startTime, use24h)}" },
            onBack = actions.onBack,
            actions = {
                if (s != null && s.session.status != SessionStatus.COMPLETED) {
                    KaijuIconButton(KaijuIcons.Edit, "Edit class", { actions.onEdit(s.session.id) })
                }
            },
        )
        when {
            state.loading -> LoadingState("Loading class", Modifier.padding(KaijuSpace.gutter))
            state.missing || s == null -> ErrorState(
                "This class isn't available",
                "It may have been deleted.",
                Modifier.padding(KaijuSpace.gutter),
                retry = StateAction("Go back", actions.onBack),
            )
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.readableWidth().padding(top = KaijuSpace.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(s.programName ?: "No program", Modifier.weight(1f))
                        SessionStatusBadge(s.session.status)
                    }
                    Spacer(Modifier.height(KaijuSpace.xs))
                    Text((s.lessonTitle ?: s.displayTitle).uppercase(), style = Kaiju.type.displayM, color = colors.ink)
                    Spacer(Modifier.height(KaijuSpace.s))
                    Text(
                        listOfNotNull(
                            Format.longDate(s.session.date),
                            Format.time(s.session.startTime, use24h),
                            Format.minutes(s.session.durationMinutes),
                            s.session.location.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = Kaiju.type.body,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(KaijuSpace.l))

                    when (s.session.status) {
                        SessionStatus.SCHEDULED, SessionStatus.IN_PROGRESS -> {
                            KaijuButton(
                                if (s.session.status == SessionStatus.IN_PROGRESS) "Resume class" else "Start class",
                                { actions.onStart(s.session.id) },
                                Modifier.fillMaxWidth(),
                                size = ButtonSize.Hero,
                                icon = KaijuIcons.Play,
                            )
                            Spacer(Modifier.height(KaijuSpace.s))
                        }
                        else -> Unit
                    }
                    if (s.session.status != SessionStatus.CANCELED) {
                        ButtonPair(
                            first = { m ->
                                KaijuButton("Attendance ${s.presentCount}/${s.expectedCount}", { actions.onAttendance(s.session.id) }, m, variant = ButtonVariant.Secondary)
                            },
                            second = { m -> KaijuButton("Add note", { noteSheet = true }, m, variant = ButtonVariant.Secondary) },
                        )
                    } else {
                        KaijuButton("Add note", { noteSheet = true }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Note)
                    }

                    val planId = s.session.lessonPlanId?.takeIf { s.session.status == SessionStatus.SCHEDULED }
                    SectionHeader(
                        if (s.session.status == SessionStatus.COMPLETED) "What was taught" else "Lesson",
                        index = 1,
                        modifier = Modifier.padding(top = KaijuSpace.xl),
                        action = if (planId != null) {
                            { KaijuButton("Open plan", { actions.onOpenLesson(planId) }, variant = ButtonVariant.Ghost, size = ButtonSize.Compact) }
                        } else {
                            null
                        },
                    )
                    state.objective?.let {
                        Text(it, style = Kaiju.type.bodyL, color = colors.ink, modifier = Modifier.padding(vertical = KaijuSpace.xs))
                    }
                    if (state.segments.isEmpty()) {
                        InlineNote("No lesson plan attached. Edit the class to add one.")
                    } else {
                        state.segments.forEachIndexed { i, seg ->
                            LessonSegmentRow(
                                index = i + 1,
                                title = seg.title,
                                minutes = seg.minutes,
                                kindLabel = seg.kindLabel,
                                techniques = seg.techniques,
                                notes = seg.notes,
                                state = if (seg.done) SegmentState.Done else SegmentState.Upcoming,
                            )
                            if (i < state.segments.lastIndex) HairlineDivider(Modifier.fillMaxWidth())
                        }
                    }

                    SectionHeader("Notes", index = 2, modifier = Modifier.padding(top = KaijuSpace.xl))
                    if (state.notes.isEmpty()) {
                        InlineNote("No notes yet.")
                    } else {
                        state.notes.forEach { note ->
                            Row(Modifier.padding(vertical = KaijuSpace.xs)) {
                                Box(Modifier.width(2.dp).height(20.dp).background(colors.gold))
                                Spacer(Modifier.width(KaijuSpace.s))
                                Text(note.body, style = Kaiju.type.body, color = colors.ink, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(KaijuSpace.xxl))
                }
            }
        }
    }
    if (noteSheet) {
        NoteSheet(onSave = { actions.onAddNote(it); noteSheet = false }, onDismiss = { noteSheet = false })
    }
}

/** Quick note entry, shared by Class Mode, class detail and the post-class screen. */
@Composable
fun NoteSheet(onSave: (String) -> Unit, onDismiss: () -> Unit, title: String = "Class note") {
    var text by remember { mutableStateOf("") }
    KaijuBottomSheet(onDismiss = onDismiss, title = title) {
        KaijuTextField(
            value = text,
            onValueChange = { text = it },
            label = "Note",
            placeholder = "What worked, who needs help, what to change next time",
            singleLine = false,
            minLines = 4,
        )
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuButton("Save note", { onSave(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank(), icon = KaijuIcons.Check)
    }
}
