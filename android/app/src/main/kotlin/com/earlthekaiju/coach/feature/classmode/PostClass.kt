package com.earlthekaiju.coach.feature.classmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.SessionNoteEntity
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KeystoneRule
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentState
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.navigation.PostClassRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.displayTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PostClassUiState(
    val title: String = "",
    val programName: String? = null,
    val actualMinutes: Int = 0,
    val plannedMinutes: Int = 0,
    val presentCount: Int = 0,
    val expectedCount: Int = 0,
    val segments: List<Pair<String, Int>> = emptyList(),
    val segmentsDone: Int = 0,
    val notes: List<SessionNoteEntity> = emptyList(),
)

class PostClassViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val sessionId = handle.toRoute<PostClassRoute>().sessionId

    val state: StateFlow<PostClassUiState> = combine(
        c.sessions.observeSummary(sessionId),
        c.sessions.observeSegments(sessionId),
        c.sessions.observeNotes(sessionId),
    ) { summary, segments, notes ->
        val s = summary?.session
        PostClassUiState(
            title = summary?.displayTitle ?: "Class",
            programName = summary?.programName,
            actualMinutes = if (s?.startedAt != null && s.endedAt != null) ((s.endedAt - s.startedAt) / 60_000).toInt() else 0,
            plannedMinutes = s?.durationMinutes ?: 0,
            presentCount = summary?.presentCount ?: 0,
            expectedCount = summary?.expectedCount ?: 0,
            segments = segments.map { it.title to it.minutes },
            segmentsDone = segments.count { it.endedAt != null },
            notes = notes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PostClassUiState())

    fun saveNote(text: String) = viewModelScope.launch { c.sessions.addNote(sessionId, text) }
}

@Composable
fun PostClassDestination(onDone: () -> Unit, onAttendance: (String) -> Unit) {
    val vm = kaijuViewModel { c, h -> PostClassViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    PostClassScreen(state, onSaveNote = { vm.saveNote(it) }, onDone = onDone, onAttendance = { onAttendance(vm.sessionId) })
}

@Composable
fun PostClassScreen(state: PostClassUiState, onSaveNote: (String) -> Unit, onDone: () -> Unit, onAttendance: () -> Unit) {
    val colors = Kaiju.colors
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = KaijuSpace.l)) {
                Eyebrow(state.programName ?: "Class")
                Text("CLASS\nCOMPLETE", style = Kaiju.type.displayL, color = colors.ink, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(KaijuSpace.s))
                KeystoneRule()
                Spacer(Modifier.height(KaijuSpace.m))
                Text(state.title, style = Kaiju.type.bodyL, color = colors.inkMuted)
                Spacer(Modifier.height(KaijuSpace.l))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Stat("Taught", "${state.actualMinutes}", "of ${state.plannedMinutes} min", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                    Stat("Present", "${state.presentCount}", "of ${state.expectedCount}", Modifier.weight(1f).padding(start = KaijuSpace.s))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                    Stat("Segments", "${state.segmentsDone}", "of ${state.segments.size}", Modifier.weight(1f).padding(start = KaijuSpace.s))
                }
                Spacer(Modifier.height(KaijuSpace.m))
                KaijuButton("Review attendance", onAttendance, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Attendance)

                SectionHeader("Notes for next time", index = 1, modifier = Modifier.padding(top = KaijuSpace.xl))
                Spacer(Modifier.height(KaijuSpace.s))
                state.notes.forEach { n ->
                    Row(Modifier.padding(vertical = KaijuSpace.xs)) {
                        Box(Modifier.width(2.dp).height(20.dp).background(colors.gold))
                        Spacer(Modifier.width(KaijuSpace.s))
                        Text(n.body, style = Kaiju.type.body, color = colors.ink)
                    }
                }
                KaijuTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Add a note",
                    placeholder = "What landed, who needs extra help, what to change",
                    singleLine = false,
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuButton(
                    "Save note",
                    { onSaveNote(note); note = "" },
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Compact,
                    enabled = note.isNotBlank(),
                    icon = KaijuIcons.Check,
                )

                SectionHeader("Taught", index = 2, modifier = Modifier.padding(top = KaijuSpace.xl))
                state.segments.forEachIndexed { i, (title, minutes) ->
                    LessonSegmentRow(index = i + 1, title = title, minutes = minutes, compact = true, state = if (i < state.segmentsDone) SegmentState.Done else SegmentState.Upcoming)
                    if (i < state.segments.lastIndex) HairlineDivider(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KaijuButton(
                "Done",
                { if (note.isNotBlank()) onSaveNote(note); onDone() },
                Modifier.readableWidth(),
                size = ButtonSize.Hero,
                icon = KaijuIcons.Check,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, suffix: String, modifier: Modifier) {
    val colors = Kaiju.colors
    Column(modifier.semantics(mergeDescendants = true) { }) {
        Eyebrow(label, color = colors.inkMuted)
        Text(value, style = Kaiju.type.numeralL, color = colors.ink)
        Text(suffix, style = Kaiju.type.caption, color = colors.inkMuted)
    }
}
