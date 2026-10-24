package com.earlthekaiju.coach.feature.students

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.StudentListItem
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.ScreenHeader
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.StudentRow
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.ui.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Roster filter: everyone active, one program, the kids, or the archive. */
sealed interface RosterFilter {
    data object Active : RosterFilter
    data object Kids : RosterFilter
    data object Archived : RosterFilter
    data class Program(val id: String) : RosterFilter
}

data class StudentsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: RosterFilter = RosterFilter.Active,
    val programs: List<ProgramEntity> = emptyList(),
    val all: List<StudentListItem> = emptyList(),
) {
    val activeCount: Int get() = all.count { it.student.status == StudentStatus.ACTIVE }

    val visible: List<StudentListItem>
        get() {
            val q = query.trim().lowercase()
            return all.filter { item ->
                val s = item.student
                val matchesQuery = q.isEmpty() || "${s.firstName} ${s.lastName}".lowercase().contains(q)
                val matchesFilter = when (val f = filter) {
                    RosterFilter.Active -> s.status == StudentStatus.ACTIVE
                    RosterFilter.Kids -> s.status == StudentStatus.ACTIVE && s.isMinor
                    RosterFilter.Archived -> s.status == StudentStatus.ARCHIVED
                    is RosterFilter.Program -> s.status == StudentStatus.ACTIVE &&
                        programs.firstOrNull { it.id == f.id }?.name?.let { name -> item.programNames.orEmpty().split(", ").contains(name) } == true
                }
                matchesQuery && matchesFilter
            }
        }
}

class StudentsViewModel(private val c: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<RosterFilter>(RosterFilter.Active)

    val state: StateFlow<StudentsUiState> = combine(
        c.students.observeList(c.clock.today().minusDays(30)),
        c.programs.observeAll(),
        query,
        filter,
    ) { students, programs, q, f ->
        StudentsUiState(loading = false, query = q, filter = f, programs = programs, all = students)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentsUiState())

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: RosterFilter) { filter.value = f }
}

data class StudentsActions(
    val onQuery: (String) -> Unit = {},
    val onFilter: (RosterFilter) -> Unit = {},
    val onOpen: (String) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun StudentsDestination(onOpen: (String) -> Unit, onAdd: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = kaijuViewModel { c, _ -> StudentsViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    StudentsScreen(state, StudentsActions(vm::setQuery, vm::setFilter, onOpen, onAdd, onOpenSettings))
}

/** Detail line for a roster row: programs and recent attendance, nothing sensitive. */
fun StudentListItem.detailLine(): String = listOfNotNull(
    programNames?.takeIf { it.isNotBlank() } ?: "No program",
    if (recentAttendance > 0) "${Format.count(recentAttendance, "class", "classes")} in 30 days" else "No classes in 30 days",
).joinToString(" · ")

@Composable
fun StudentsScreen(state: StudentsUiState, actions: StudentsActions) {
    val colors = Kaiju.colors
    LazyColumn(
        Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(vertical = KaijuSpace.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Students",
                eyebrow = "${state.activeCount} active · ${Format.count(state.programs.size, "program")}",
                modifier = Modifier.readableWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = KaijuSpace.gutter),
                trailing = {
                    KaijuIconButton(KaijuIcons.Add, "Add student", actions.onAdd, filled = true)
                    KaijuIconButton(KaijuIcons.Settings, "Settings", actions.onOpenSettings, tint = colors.inkMuted)
                },
            )
        }
        if (state.loading) {
            item { LoadingState("Loading roster", Modifier.padding(KaijuSpace.gutter)) }
            return@LazyColumn
        }
        if (state.all.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    eyebrow = "Roster",
                    title = "No students yet",
                    body = "Add the people you teach. Only what you need on the mat: name, belt and program. No contact or medical details.",
                    primary = StateAction("Add student", actions.onAdd),
                    modifier = Modifier.readableWidth().padding(KaijuSpace.gutter),
                )
            }
            return@LazyColumn
        }
        item(key = "search") {
            KaijuTextField(
                value = state.query,
                onValueChange = actions.onQuery,
                label = "Search",
                placeholder = "Name",
                leadingIcon = KaijuIcons.Search,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Search),
                trailing = if (state.query.isNotEmpty()) {
                    { KaijuIconButton(KaijuIcons.Close, "Clear search", { actions.onQuery("") }) }
                } else {
                    null
                },
                modifier = Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter),
            )
        }
        item(key = "filters") {
            Row(
                Modifier
                    .readableWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
                horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs),
            ) {
                KaijuChip("Active", state.filter == RosterFilter.Active, { actions.onFilter(RosterFilter.Active) })
                state.programs.forEach { p ->
                    KaijuChip(p.name, state.filter == RosterFilter.Program(p.id), { actions.onFilter(RosterFilter.Program(p.id)) })
                }
                KaijuChip("Kids", state.filter == RosterFilter.Kids, { actions.onFilter(RosterFilter.Kids) })
                KaijuChip("Archived", state.filter == RosterFilter.Archived, { actions.onFilter(RosterFilter.Archived) })
            }
        }
        item(key = "count") {
            HairlineDivider(Modifier.readableWidth())
        }
        val visible = state.visible
        if (visible.isEmpty()) {
            item(key = "none") {
                InlineNote(
                    if (state.query.isNotBlank()) "No one matches \"${state.query}\"." else "No students in this view.",
                    Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter),
                )
            }
        }
        items(visible, key = { it.student.id }) { item ->
            Column(Modifier.readableWidth()) {
                StudentRow(
                    name = "${item.student.firstName} ${item.student.lastName}".trim(),
                    belt = item.student.belt,
                    stripes = item.student.stripes,
                    detail = item.detailLine(),
                    archived = item.student.status == StudentStatus.ARCHIVED,
                    isMinor = item.student.isMinor,
                    onClick = { actions.onOpen(item.student.id) },
                )
                HairlineDivider(Modifier.fillMaxWidth().padding(horizontal = KaijuSpace.gutter))
            }
        }
        item(key = "add") {
            KaijuButton(
                "Add student",
                actions.onAdd,
                Modifier.readableWidth().padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.l),
                icon = KaijuIcons.Add,
            )
            Spacer(Modifier.height(KaijuSpace.l))
        }
    }
}
