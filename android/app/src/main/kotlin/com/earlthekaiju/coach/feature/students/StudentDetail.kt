package com.earlthekaiju.coach.feature.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.StudentAttendanceRow
import com.earlthekaiju.coach.data.db.StudentEntity
import com.earlthekaiju.coach.designsystem.component.BeltBar
import com.earlthekaiju.coach.designsystem.component.ErrorState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.navigation.StudentDetailRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StudentDetailUiState(
    val loading: Boolean = true,
    val student: StudentEntity? = null,
    val programs: List<ProgramEntity> = emptyList(),
    val history: List<StudentAttendanceRow> = emptyList(),
    val today: LocalDate = LocalDate.now(),
) {
    val presentTotal: Int get() = history.count { it.status == AttendanceStatus.PRESENT }
    val present30: Int get() = history.count { it.status == AttendanceStatus.PRESENT && !it.date.isBefore(today.minusDays(30)) }
    val lastPresent: LocalDate? get() = history.firstOrNull { it.status == AttendanceStatus.PRESENT }?.date
}

class StudentDetailViewModel(c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val studentId = handle.toRoute<StudentDetailRoute>().studentId
    private val today = c.clock.today()

    val state: StateFlow<StudentDetailUiState> = combine(
        c.students.observe(studentId),
        c.students.observeEnrollments(studentId),
        c.programs.observeAll(),
        c.students.observeHistory(studentId),
    ) { student, enrolled, programs, history ->
        StudentDetailUiState(
            loading = false,
            student = student,
            programs = programs.filter { it.id in enrolled },
            history = history,
            today = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentDetailUiState())
}

@Composable
fun StudentDetailDestination(onBack: () -> Unit, onEdit: (String) -> Unit, onOpenSession: (String) -> Unit) {
    val vm = kaijuViewModel { c, h -> StudentDetailViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    StudentDetailScreen(state, onBack, { onEdit(vm.studentId) }, onOpenSession)
}

@Composable
fun StudentDetailScreen(state: StudentDetailUiState, onBack: () -> Unit, onEdit: () -> Unit, onOpenSession: (String) -> Unit) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    val s = state.student
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = s?.let { "${it.firstName} ${it.lastName}".trim() } ?: "Student",
            eyebrow = if (s?.status == StudentStatus.ARCHIVED) "Archived" else "Student",
            onBack = onBack,
            actions = { if (s != null) KaijuIconButton(KaijuIcons.Edit, "Edit student", onEdit) },
        )
        when {
            state.loading -> LoadingState("Loading student", Modifier.padding(KaijuSpace.gutter))
            s == null -> ErrorState("Student not found", "They may have been deleted.", Modifier.padding(KaijuSpace.gutter), StateAction("Go back", onBack))
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.readableWidth().padding(top = KaijuSpace.l)) {
                    BeltBar(s.belt, s.stripes, width = 160.dp, height = 28.dp)
                    Spacer(Modifier.height(KaijuSpace.s))
                    Text(s.belt.describe(s.stripes), style = Kaiju.type.title, color = colors.ink)
                    Text(
                        listOfNotNull(
                            state.programs.joinToString(", ") { it.name }.ifEmpty { "No program" },
                            if (s.isMinor) "Kids" else null,
                        ).joinToString(" · "),
                        style = Kaiju.type.body,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(KaijuSpace.l))
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Stat("Last 30 days", state.present30.toString(), "classes", Modifier.weight(1f))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                        Stat("All time", state.presentTotal.toString(), "classes", Modifier.weight(1f).padding(start = KaijuSpace.s))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                        val daysAgo = state.lastPresent?.let { java.time.temporal.ChronoUnit.DAYS.between(it, state.today) }
                        Stat(
                            "Last seen",
                            when (daysAgo) { null -> "—"; 0L -> "Today"; else -> daysAgo.toString() },
                            when (daysAgo) { null -> "no classes yet"; 0L -> "in class"; 1L -> "day ago"; else -> "days ago" },
                            Modifier.weight(1f).padding(start = KaijuSpace.s),
                        )
                    }
                    if (s.notes.isNotBlank()) {
                        SectionHeader("Teaching note", index = 1, modifier = Modifier.padding(top = KaijuSpace.xl))
                        Text(s.notes, style = Kaiju.type.bodyL, color = colors.ink, modifier = Modifier.padding(top = KaijuSpace.xs))
                    }
                    SectionHeader("Attendance history", index = if (s.notes.isNotBlank()) 2 else 1, modifier = Modifier.padding(top = KaijuSpace.xl))
                    if (state.history.isEmpty()) InlineNote("No attendance recorded yet.")
                    state.history.forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(vertical = KaijuSpace.xs)
                                .semantics(mergeDescendants = true) { },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val present = row.status == AttendanceStatus.PRESENT
                            Icon(
                                if (present) KaijuIcons.Check else KaijuIcons.Close,
                                contentDescription = row.status.label,
                                tint = if (present) colors.accent else colors.signal,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(KaijuSpace.s))
                            Column(Modifier.weight(1f)) {
                                Text(row.title.ifBlank { row.programName ?: "Class" }, style = Kaiju.type.titleSmall, color = colors.ink)
                                Text("${Format.shortDate(row.date)} · ${Format.time(row.startTime, use24h)}", style = Kaiju.type.caption, color = colors.inkMuted)
                            }
                            Text(row.status.label.uppercase(), style = Kaiju.type.eyebrow, color = if (present) colors.accent else colors.signal)
                            KaijuIconButton(KaijuIcons.ChevronRight, "Open class", { onOpenSession(row.sessionId) }, tint = colors.inkMuted)
                        }
                        HairlineDivider(Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(KaijuSpace.xxl))
                }
            }
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
