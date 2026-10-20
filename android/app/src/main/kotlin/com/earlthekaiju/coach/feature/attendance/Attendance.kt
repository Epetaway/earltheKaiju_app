package com.earlthekaiju.coach.feature.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.RosterRow
import com.earlthekaiju.coach.data.db.SessionSummary
import com.earlthekaiju.coach.data.db.StudentListItem
import com.earlthekaiju.coach.designsystem.component.AttendanceRow
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.StudentRow
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.navigation.AttendanceRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalSnackbarHost
import com.earlthekaiju.coach.ui.displayTitle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val loading: Boolean = true,
    val session: SessionSummary? = null,
    val roster: List<RosterRow> = emptyList(),
    /** Active students not on this roster, for adding drop-ins. */
    val others: List<StudentListItem> = emptyList(),
) {
    val present: Int get() = roster.count { it.status == AttendanceStatus.PRESENT }
    val absent: Int get() = roster.count { it.status == AttendanceStatus.ABSENT }
    val unmarked: Int get() = roster.count { it.status == null }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val sessionId = handle.toRoute<AttendanceRoute>().sessionId

    val state: StateFlow<AttendanceUiState> = c.sessions.observeSummary(sessionId).flatMapLatest { summary ->
        if (summary == null) return@flatMapLatest flowOf(AttendanceUiState(loading = false))
        combine(
            c.attendance.observeRoster(sessionId, summary.session.programId),
            c.students.observeList(c.clock.today().minusDays(30)),
        ) { roster, students ->
            val onRoster = roster.map { it.studentId }.toSet()
            AttendanceUiState(
                loading = false,
                session = summary,
                roster = roster,
                others = students.filter { it.student.status == StudentStatus.ACTIVE && it.student.id !in onRoster },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttendanceUiState())

    /** Sets a mark; tapping the current mark again clears it. Returns (previous, new) for Undo. */
    suspend fun toggle(studentId: String, status: AttendanceStatus): Pair<AttendanceStatus?, AttendanceStatus?> {
        val current = state.value.roster.firstOrNull { it.studentId == studentId }?.status
        val next = if (current == status) null else status
        val previous = c.attendance.mark(sessionId, studentId, next)
        return previous to next
    }

    fun restore(studentId: String, status: AttendanceStatus?) = viewModelScope.launch { c.attendance.mark(sessionId, studentId, status) }

    suspend fun markAllPresent(): Map<String, AttendanceStatus?> {
        val ids = state.value.roster.filter { it.status == null }.map { it.studentId }
        return c.attendance.markAll(sessionId, ids, AttendanceStatus.PRESENT)
    }

    fun restoreAll(previous: Map<String, AttendanceStatus?>) = viewModelScope.launch { c.attendance.restore(sessionId, previous) }
}

data class AttendanceActions(
    val onBack: () -> Unit = {},
    val onMark: (RosterRow, AttendanceStatus) -> Unit = { _, _ -> },
    val onMarkAllPresent: () -> Unit = {},
    val onAddDropIn: (String) -> Unit = {},
)

@Composable
fun AttendanceDestination(onBack: () -> Unit) {
    val vm = kaijuViewModel { c, h -> AttendanceViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    AttendanceScreen(
        state,
        AttendanceActions(
            onBack = onBack,
            onMark = { row, status ->
                scope.launch {
                    val (previous, next) = vm.toggle(row.studentId, status)
                    val name = row.firstName
                    val message = when (next) {
                        null -> "Cleared $name"
                        AttendanceStatus.PRESENT -> "$name marked present"
                        AttendanceStatus.ABSENT -> "$name marked absent"
                    }
                    if (snackbar.showUndo(message) == SnackbarResult.ActionPerformed) vm.restore(row.studentId, previous)
                }
            },
            onMarkAllPresent = {
                scope.launch {
                    val previous = vm.markAllPresent()
                    if (previous.isNotEmpty() &&
                        snackbar.showUndo("${Format.count(previous.size, "student")} marked present") == SnackbarResult.ActionPerformed
                    ) {
                        vm.restoreAll(previous)
                    }
                }
            },
            onAddDropIn = { id -> scope.launch { vm.toggle(id, AttendanceStatus.PRESENT) } },
        ),
    )
}

private suspend fun SnackbarHostState.showUndo(message: String): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(message = message, actionLabel = "Undo", withDismissAction = false, duration = SnackbarDuration.Short)
}

@Composable
fun AttendanceScreen(state: AttendanceUiState, actions: AttendanceActions) {
    val colors = Kaiju.colors
    var dropIn by remember { mutableStateOf(false) }
    val s = state.session
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = "Attendance",
            eyebrow = s?.let { "${Format.dayEyebrow(it.session.date)} · ${it.displayTitle}" },
            onBack = actions.onBack,
        )
        if (state.loading) {
            LoadingState("Loading roster", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = KaijuSpace.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "totals") {
                Totals(state, Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.m))
            }
            item(key = "bulk") {
                Row(
                    Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter),
                    horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s),
                ) {
                    KaijuButton(
                        "All present",
                        actions.onMarkAllPresent,
                        Modifier.weight(1f),
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Compact,
                        icon = KaijuIcons.Check,
                        enabled = state.unmarked > 0,
                    )
                    KaijuButton(
                        "Add drop-in",
                        { dropIn = true },
                        Modifier.weight(1f),
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Compact,
                        icon = KaijuIcons.Add,
                        enabled = state.others.isNotEmpty(),
                    )
                }
                Spacer(Modifier.height(KaijuSpace.s))
                HairlineDivider(Modifier.readableWidth())
            }
            if (state.roster.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        eyebrow = "No roster",
                        title = if (s?.session?.programId == null) "This class has no program" else "No one enrolled yet",
                        body = if (s?.session?.programId == null) {
                            "Add the students who showed up as drop-ins, or give the class a program so its roster appears here."
                        } else {
                            "Enroll students in this program from the Students tab, or add drop-ins now."
                        },
                        primary = if (state.others.isNotEmpty()) StateAction("Add drop-in", { dropIn = true }) else null,
                        showFrame = false,
                        modifier = Modifier.readableWidth().padding(KaijuSpace.gutter),
                    )
                }
            }
            items(state.roster, key = { it.studentId }) { row ->
                Column(Modifier.readableWidth()) {
                    AttendanceRow(
                        name = "${row.firstName} ${row.lastName}".trim(),
                        belt = row.belt,
                        stripes = row.stripes,
                        status = row.status,
                        onMark = { actions.onMark(row, it) },
                        note = if (!row.enrolled) "Drop-in" else null,
                    )
                    HairlineDivider(Modifier.fillMaxWidth().padding(horizontal = KaijuSpace.gutter))
                }
            }
            item(key = "hint") {
                InlineNote(
                    "Tap a mark again to clear it. Changes save instantly and survive closing the app.",
                    Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter),
                )
            }
        }
    }
    if (dropIn) {
        DropInSheet(state.others, onPick = { actions.onAddDropIn(it); dropIn = false }, onDismiss = { dropIn = false })
    }
}

@Composable
private fun Totals(state: AttendanceUiState, modifier: Modifier) {
    val colors = Kaiju.colors
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "${state.present} present, ${state.absent} absent, ${state.unmarked} not marked"
            },
    ) {
        Total("Present", state.present, colors.ink, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
        Total("Absent", state.absent, colors.ink, Modifier.weight(1f).padding(start = KaijuSpace.s))
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
        Total("Not marked", state.unmarked, if (state.unmarked > 0) colors.gold else colors.inkMuted, Modifier.weight(1f).padding(start = KaijuSpace.s))
    }
}

@Composable
private fun Total(label: String, value: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(value.toString(), style = Kaiju.type.numeralL, color = color)
        Eyebrow(label, color = Kaiju.colors.inkMuted)
    }
}

@Composable
private fun DropInSheet(students: List<StudentListItem>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    KaijuBottomSheet(onDismiss = onDismiss, title = "Add drop-in", eyebrow = "Attendance") {
        KaijuTextField(query, { query = it }, "Search", placeholder = "Name", leadingIcon = KaijuIcons.Search)
        Spacer(Modifier.height(KaijuSpace.s))
        val filtered = students.filter { "${it.student.firstName} ${it.student.lastName}".contains(query.trim(), ignoreCase = true) }
        if (filtered.isEmpty()) InlineNote("No matching students.")
        filtered.take(40).forEach { item ->
            StudentRow(
                name = "${item.student.firstName} ${item.student.lastName}".trim(),
                belt = item.student.belt,
                stripes = item.student.stripes,
                detail = item.programNames ?: "No program",
                onClick = { onPick(item.student.id) },
                horizontalPadding = 0.dp,
            )
            HairlineDivider(Modifier.fillMaxWidth())
        }
    }
}
