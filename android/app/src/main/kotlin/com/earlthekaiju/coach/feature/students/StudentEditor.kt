package com.earlthekaiju.coach.feature.students

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.repo.StudentDraft
import com.earlthekaiju.coach.designsystem.component.BeltBar
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuSwitchRow
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.NumberStepper
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.StudentStatus
import com.earlthekaiju.coach.navigation.StudentEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudentEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: StudentDraft = StudentDraft(),
    val dirty: Boolean = false,
    val programs: List<ProgramEntity> = emptyList(),
    val nameError: String? = null,
    val done: Boolean = false,
)

class StudentEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val studentId = handle.toRoute<StudentEditorRoute>().studentId
    private val form = MutableStateFlow(StudentEditorUiState())
    private var original: StudentDraft? = null

    val state: StateFlow<StudentEditorUiState> = combine(form, c.programs.observeAll()) { f, programs -> f.copy(programs = programs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        viewModelScope.launch {
            val loaded = studentId?.let { c.students.loadDraft(it) }
            val draft = loaded ?: StudentDraft()
            original = draft
            form.value = form.value.copy(loading = false, isNew = loaded == null, draft = draft)
        }
    }

    fun update(transform: (StudentDraft) -> StudentDraft) {
        val next = transform(form.value.draft).let { d -> d.copy(stripes = d.stripes.coerceIn(0, d.belt.maxStripes)) }
        form.value = form.value.copy(draft = next, dirty = next != original, nameError = null)
    }

    fun save() = viewModelScope.launch {
        val d = form.value.draft
        if (d.firstName.isBlank()) {
            form.value = form.value.copy(nameError = "Add at least a first name.")
            return@launch
        }
        c.students.save(d)
        form.value = form.value.copy(done = true)
    }

    fun setArchived(archived: Boolean) = viewModelScope.launch {
        val id = form.value.draft.id ?: return@launch
        c.students.save(form.value.draft)
        c.students.setArchived(id, archived)
        form.value = form.value.copy(done = true)
    }

    fun deletePermanently() = viewModelScope.launch {
        form.value.draft.id?.let { c.students.deletePermanently(it) }
        form.value = form.value.copy(done = true)
    }
}

data class StudentEditorActions(
    val onClose: () -> Unit = {},
    val onUpdate: ((StudentDraft) -> StudentDraft) -> Unit = {},
    val onSave: () -> Unit = {},
    val onArchive: (Boolean) -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun StudentEditorDestination(onClose: () -> Unit, onDeleted: () -> Unit) {
    val vm = kaijuViewModel { c, h -> StudentEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    var deleted by remember { mutableStateOf(false) }
    LaunchedEffect(state.done) { if (state.done) { if (deleted) onDeleted() else onClose() } }
    StudentEditorScreen(
        state,
        StudentEditorActions(
            onClose = onClose,
            onUpdate = vm::update,
            onSave = { vm.save() },
            onArchive = { vm.setArchived(it) },
            onDelete = { deleted = true; vm.deletePermanently() },
        ),
    )
}

private enum class StudentDialog { None, Discard, Delete }

@Composable
fun StudentEditorScreen(state: StudentEditorUiState, actions: StudentEditorActions) {
    val colors = Kaiju.colors
    var dialog by remember { mutableStateOf(StudentDialog.None) }
    val d = state.draft
    BackHandler(enabled = state.dirty) { dialog = StudentDialog.Discard }
    // imePadding on the whole screen: with the keyboard up the scroll area shrinks (keeping the
    // focused field in view) and the bottom action bar sits right above the keyboard.
    Column(Modifier.fillMaxSize().background(colors.background).imePadding()) {
        KaijuTopBar(
            title = if (state.isNew) "Add student" else "${d.firstName} ${d.lastName}".trim().ifEmpty { "Student" },
            eyebrow = "Roster",
            onBack = { if (state.dirty) dialog = StudentDialog.Discard else actions.onClose() },
            actions = { KaijuButton("Save", actions.onSave, variant = ButtonVariant.Ghost, size = ButtonSize.Compact) },
        )
        if (state.loading) {
            LoadingState("Loading student", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth()) {
                SectionHeader("Name", index = 1, modifier = Modifier.padding(top = KaijuSpace.m))
                Spacer(Modifier.height(KaijuSpace.s))
                Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
                    KaijuTextField(
                        d.firstName, { v -> actions.onUpdate { it.copy(firstName = v) } }, "First name",
                        Modifier.weight(1f),
                        errorText = state.nameError,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    )
                    KaijuTextField(
                        d.lastName, { v -> actions.onUpdate { it.copy(lastName = v) } }, "Last name",
                        Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                }
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuSwitchRow(
                    title = "Under 18",
                    subtitle = "Kids' belts and a KIDS marker on the roster. No birth date is stored.",
                    checked = d.isMinor,
                    onCheckedChange = { v -> actions.onUpdate { it.copy(isMinor = v, belt = if (!v && it.belt.isKids) Belt.WHITE else it.belt) } },
                )

                SectionHeader("Rank", index = 2, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                val beltOptions = if (d.isMinor) Belt.kids else Belt.adults
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BeltBar(d.belt, d.stripes, width = 96.dp, height = 22.dp)
                    Spacer(Modifier.width(KaijuSpace.s))
                    Text(d.belt.describe(d.stripes), style = Kaiju.type.titleSmall, color = colors.ink)
                }
                Spacer(Modifier.height(KaijuSpace.s))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                    beltOptions.forEach { b -> KaijuChip(b.label, d.belt == b, { actions.onUpdate { it.copy(belt = b) } }) }
                }
                NumberStepper(
                    label = if (d.belt == Belt.BLACK) "Degrees" else "Stripes",
                    value = d.stripes,
                    onValueChange = { v -> actions.onUpdate { it.copy(stripes = v) } },
                    range = 0..d.belt.maxStripes,
                )

                SectionHeader("Programs", index = 3, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                if (state.programs.isEmpty()) {
                    InlineNote("No programs yet. Add them in Settings › Programs so classes know who to expect.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                        state.programs.forEach { p ->
                            KaijuChip(p.name, p.id in d.programIds, {
                                actions.onUpdate { it.copy(programIds = if (p.id in it.programIds) it.programIds - p.id else it.programIds + p.id) }
                            })
                        }
                    }
                    Text("Students are expected at every class of their programs.", style = Kaiju.type.caption, color = colors.inkMuted, modifier = Modifier.padding(top = KaijuSpace.xxs))
                }

                SectionHeader("Teaching note", index = 4, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuTextField(
                    value = d.notes,
                    onValueChange = { v -> actions.onUpdate { it.copy(notes = v) } },
                    label = "Note",
                    placeholder = "e.g. Working on frames from bottom side control",
                    supportingText = "Teaching notes only. Don't store medical, contact or family details here.",
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                if (!state.isNew) {
                    SectionHeader("Manage", index = 5, modifier = Modifier.padding(top = KaijuSpace.l))
                    Spacer(Modifier.height(KaijuSpace.s))
                    if (d.status == StudentStatus.ACTIVE) {
                        KaijuButton("Archive student", { actions.onArchive(true) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Archive)
                        Text("Archived students leave the roster but keep their attendance history.", style = Kaiju.type.caption, color = colors.inkMuted, modifier = Modifier.padding(top = KaijuSpace.xxs))
                    } else {
                        KaijuButton("Restore to roster", { actions.onArchive(false) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Undo)
                    }
                    Spacer(Modifier.height(KaijuSpace.s))
                    KaijuButton("Delete permanently", { dialog = StudentDialog.Delete }, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KaijuButton(if (state.isNew) "Add to roster" else "Save changes", actions.onSave, Modifier.readableWidth(), icon = KaijuIcons.Check)
        }
    }
    when (dialog) {
        StudentDialog.Discard -> ConfirmDialog(
            title = "Discard changes?",
            body = "Your edits haven't been saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = { dialog = StudentDialog.None; actions.onClose() },
            onDismiss = { dialog = StudentDialog.None },
        )
        StudentDialog.Delete -> ConfirmDialog(
            title = "Delete permanently?",
            body = "Removes this student's name, rank, notes and attendance marks from the app. Use Archive instead if they might come back.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { dialog = StudentDialog.None; actions.onDelete() },
            onDismiss = { dialog = StudentDialog.None },
        )
        StudentDialog.None -> Unit
    }
}
