package com.earlthekaiju.coach.feature.lessons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.TechniqueWithUsage
import com.earlthekaiju.coach.data.repo.LessonPlanDraft
import com.earlthekaiju.coach.data.repo.SegmentDraft
import com.earlthekaiju.coach.data.repo.TechniqueDraft
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.InlineNote
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuSwitchRow
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.NumberStepper
import com.earlthekaiju.coach.designsystem.component.OptionSheet
import com.earlthekaiju.coach.designsystem.component.PickerField
import com.earlthekaiju.coach.designsystem.component.PickerOption
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.indexLabel
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.SegmentKind
import com.earlthekaiju.coach.navigation.LessonEditorRoute
import com.earlthekaiju.coach.ui.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LessonEditorExit {
    data object Closed : LessonEditorExit
    data class AssignToClass(val planId: String) : LessonEditorExit
}

data class LessonEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: LessonPlanDraft = LessonPlanDraft(),
    val dirty: Boolean = false,
    val programs: List<ProgramEntity> = emptyList(),
    val techniques: List<TechniqueWithUsage> = emptyList(),
    val scheduledUsingPlan: Int = 0,
    val titleError: String? = null,
    val exit: LessonEditorExit? = null,
) {
    val programName: String? get() = programs.firstOrNull { it.id == draft.programId }?.name
    fun techniqueName(id: String): String? = techniques.firstOrNull { it.technique.id == id }?.technique?.name
}

class LessonEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val planId = handle.toRoute<LessonEditorRoute>().lessonPlanId
    private val form = MutableStateFlow(LessonEditorUiState())
    private var original: LessonPlanDraft? = null

    val state: StateFlow<LessonEditorUiState> =
        combine(form, c.programs.observeAll(), c.techniques.observeAll()) { f, programs, techniques ->
            f.copy(programs = programs, techniques = techniques)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        viewModelScope.launch {
            val loaded = planId?.let { c.lessons.loadDraft(it) }
            val draft = loaded ?: LessonPlanDraft(
                segments = listOf(
                    SegmentDraft(title = "Warm-up", minutes = 10, kind = SegmentKind.WARM_UP),
                    SegmentDraft(title = "", minutes = 20, kind = SegmentKind.INSTRUCTION),
                ),
            )
            original = draft
            form.value = form.value.copy(
                loading = false,
                isNew = loaded == null,
                draft = draft,
                scheduledUsingPlan = planId?.let { c.lessons.countScheduledUsing(it) } ?: 0,
            )
        }
    }

    private fun edit(transform: (LessonPlanDraft) -> LessonPlanDraft) {
        val next = transform(form.value.draft)
        form.value = form.value.copy(draft = next, dirty = next != original, titleError = null)
    }

    fun update(transform: (LessonPlanDraft) -> LessonPlanDraft) = edit(transform)

    fun addSegment(): String {
        val segment = SegmentDraft(minutes = 10)
        edit { it.copy(segments = it.segments + segment) }
        return segment.id
    }

    fun replaceSegment(segment: SegmentDraft) = edit { d -> d.copy(segments = d.segments.map { if (it.id == segment.id) segment else it }) }

    fun removeSegment(id: String) = edit { d -> d.copy(segments = d.segments.filterNot { it.id == id }) }

    fun moveSegment(id: String, delta: Int) = edit { d ->
        val list = d.segments.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@edit d
        list.add(to, list.removeAt(from))
        d.copy(segments = list)
    }

    suspend fun createTechnique(name: String): String = c.techniques.save(TechniqueDraft(name = name))

    private suspend fun persist(): String? {
        val d = form.value.draft
        if (d.title.isBlank()) {
            form.value = form.value.copy(titleError = "Give the lesson a title.")
            return null
        }
        val id = c.lessons.save(d)
        original = d.copy(id = id)
        form.value = form.value.copy(draft = d.copy(id = id), dirty = false, isNew = false)
        return id
    }

    fun save() = viewModelScope.launch { if (persist() != null) form.value = form.value.copy(exit = LessonEditorExit.Closed) }

    fun saveAndAssign() = viewModelScope.launch {
        persist()?.let { form.value = form.value.copy(exit = LessonEditorExit.AssignToClass(it)) }
    }

    fun duplicate() = viewModelScope.launch {
        val id = persist() ?: return@launch
        c.lessons.duplicate(id)
        form.value = form.value.copy(exit = LessonEditorExit.Closed)
    }

    fun delete() = viewModelScope.launch {
        form.value.draft.id?.let { c.lessons.delete(it) }
        form.value = form.value.copy(exit = LessonEditorExit.Closed)
    }
}

data class LessonEditorActions(
    val onClose: () -> Unit = {},
    val onUpdate: ((LessonPlanDraft) -> LessonPlanDraft) -> Unit = {},
    val onAddSegment: () -> String = { "" },
    val onReplaceSegment: (SegmentDraft) -> Unit = {},
    val onRemoveSegment: (String) -> Unit = {},
    val onMoveSegment: (String, Int) -> Unit = { _, _ -> },
    val onCreateTechnique: suspend (String) -> String = { "" },
    val onSave: () -> Unit = {},
    val onSaveAndAssign: () -> Unit = {},
    val onDuplicate: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun LessonEditorDestination(onClose: () -> Unit, onAssign: (String) -> Unit) {
    val vm = kaijuViewModel { c, h -> LessonEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.exit) {
        when (val exit = state.exit) {
            LessonEditorExit.Closed -> onClose()
            is LessonEditorExit.AssignToClass -> onAssign(exit.planId)
            null -> Unit
        }
    }
    LessonEditorScreen(
        state,
        LessonEditorActions(
            onClose = onClose,
            onUpdate = vm::update,
            onAddSegment = vm::addSegment,
            onReplaceSegment = vm::replaceSegment,
            onRemoveSegment = vm::removeSegment,
            onMoveSegment = vm::moveSegment,
            onCreateTechnique = vm::createTechnique,
            onSave = { vm.save() },
            onSaveAndAssign = { vm.saveAndAssign() },
            onDuplicate = { vm.duplicate() },
            onDelete = { vm.delete() },
        ),
    )
}

private sealed interface LessonDialog {
    data object None : LessonDialog
    data object Program : LessonDialog
    data object Discard : LessonDialog
    data object Delete : LessonDialog
    data class Segment(val id: String) : LessonDialog
}

@Composable
fun LessonEditorScreen(state: LessonEditorUiState, actions: LessonEditorActions) {
    val colors = Kaiju.colors
    var dialog by remember { mutableStateOf<LessonDialog>(LessonDialog.None) }
    val d = state.draft
    val back = { if (state.dirty) dialog = LessonDialog.Discard else actions.onClose() }
    BackHandler(enabled = state.dirty) { dialog = LessonDialog.Discard }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = if (state.isNew) "New lesson plan" else d.title.ifBlank { "Lesson plan" },
            eyebrow = "Lesson builder",
            onBack = back,
            actions = { KaijuButton("Save", actions.onSave, variant = ButtonVariant.Ghost, size = ButtonSize.Compact) },
        )
        if (state.loading) {
            LoadingState("Loading lesson", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth()) {
                SectionHeader("Details", index = 1, modifier = Modifier.padding(top = KaijuSpace.m))
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuTextField(
                    value = d.title,
                    onValueChange = { v -> actions.onUpdate { it.copy(title = v) } },
                    label = "Lesson title",
                    placeholder = "e.g. Guard Retention",
                    errorText = state.titleError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Spacer(Modifier.height(KaijuSpace.m))
                PickerField("Program or audience", state.programName ?: "", { dialog = LessonDialog.Program }, placeholder = "Any program")
                Spacer(Modifier.height(KaijuSpace.s))
                NumberStepper(
                    label = "Target length",
                    value = d.targetMinutes,
                    onValueChange = { v -> actions.onUpdate { it.copy(targetMinutes = v) } },
                    range = 15..240,
                    step = 5,
                    unitLabel = "minutes",
                )
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuTextField(
                    value = d.objective,
                    onValueChange = { v -> actions.onUpdate { it.copy(objective = v) } },
                    label = "Teaching objective",
                    placeholder = "What should students leave able to do?",
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                SectionHeader("Segments", index = 2, modifier = Modifier.padding(top = KaijuSpace.xl))
                TimeBudget(planned = d.segmentMinutes, target = d.targetMinutes, segments = d.segments.map { it.minutes }, modifier = Modifier.padding(vertical = KaijuSpace.s))
                if (d.segments.isEmpty()) {
                    InlineNote("No segments yet. Add the blocks of your class in the order you teach them.")
                }
                d.segments.forEachIndexed { i, seg ->
                    SegmentEditorRow(
                        index = i,
                        count = d.segments.size,
                        segment = seg,
                        techniqueNames = seg.techniqueIds.mapNotNull(state::techniqueName),
                        onEdit = { dialog = LessonDialog.Segment(seg.id) },
                        onMove = { delta -> actions.onMoveSegment(seg.id, delta) },
                    )
                    HairlineDivider(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuButton(
                    "Add segment",
                    { dialog = LessonDialog.Segment(actions.onAddSegment()) },
                    Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    icon = KaijuIcons.Add,
                )

                SectionHeader("Notes", index = 3, modifier = Modifier.padding(top = KaijuSpace.xl))
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuTextField(
                    value = d.notes,
                    onValueChange = { v -> actions.onUpdate { it.copy(notes = v) } },
                    label = "Instructor notes",
                    placeholder = "Reminders, safety points, variations for beginners",
                    singleLine = false,
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuSwitchRow(
                    title = "Keep in lesson library",
                    subtitle = "Reusable for future classes. Turn off for a one-off plan.",
                    checked = d.isTemplate,
                    onCheckedChange = { v -> actions.onUpdate { it.copy(isTemplate = v) } },
                )

                SectionHeader("Use", index = 4, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuButton("Save and assign to a class", actions.onSaveAndAssign, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Planner)
                if (!state.isNew) {
                    Spacer(Modifier.height(KaijuSpace.s))
                    KaijuButton("Duplicate plan", actions.onDuplicate, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Copy)
                    Spacer(Modifier.height(KaijuSpace.s))
                    KaijuButton("Delete plan", { dialog = LessonDialog.Delete }, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KaijuButton(if (state.isNew) "Save lesson plan" else "Save changes", actions.onSave, Modifier.readableWidth(), icon = KaijuIcons.Check)
        }
    }

    when (val dl = dialog) {
        LessonDialog.Program -> OptionSheet(
            title = "Program or audience",
            options = listOf(PickerOption(null, "Any program")) + state.programs.map { PickerOption(it.id, it.name, it.audience.label) },
            selectedId = d.programId,
            onSelect = { id -> actions.onUpdate { it.copy(programId = id) }; dialog = LessonDialog.None },
            onDismiss = { dialog = LessonDialog.None },
        )
        LessonDialog.Discard -> ConfirmDialog(
            title = "Discard changes?",
            body = "Your edits to this lesson plan haven't been saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = { dialog = LessonDialog.None; actions.onClose() },
            onDismiss = { dialog = LessonDialog.None },
        )
        LessonDialog.Delete -> ConfirmDialog(
            title = "Delete this lesson plan?",
            body = if (state.scheduledUsingPlan > 0) {
                "${Format.count(state.scheduledUsingPlan, "scheduled class", "scheduled classes")} will lose this plan. Classes already taught keep what was taught."
            } else {
                "Classes already taught keep what was taught."
            },
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { dialog = LessonDialog.None; actions.onDelete() },
            onDismiss = { dialog = LessonDialog.None },
        )
        is LessonDialog.Segment -> {
            val seg = d.segments.firstOrNull { it.id == dl.id }
            if (seg == null) {
                dialog = LessonDialog.None
            } else {
                SegmentSheet(
                    segment = seg,
                    position = d.segments.indexOf(seg) + 1,
                    techniques = state.techniques,
                    onDone = { actions.onReplaceSegment(it); dialog = LessonDialog.None },
                    onDelete = { actions.onRemoveSegment(seg.id); dialog = LessonDialog.None },
                    onCreateTechnique = actions.onCreateTechnique,
                    onDismiss = { dialog = LessonDialog.None },
                )
            }
        }
        LessonDialog.None -> Unit
    }
}

/** Planned minutes against the target, as proportional blocks plus words (never color alone). */
@Composable
private fun TimeBudget(planned: Int, target: Int, segments: List<Int>, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val over = planned - target
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$planned", style = Kaiju.type.numeralM, color = colors.ink)
            Text(" / $target MIN PLANNED", style = Kaiju.type.eyebrow, color = colors.inkMuted, modifier = Modifier.padding(bottom = 5.dp).weight(1f))
            Text(
                when {
                    over > 0 -> "${over} MIN OVER"
                    over < 0 -> "${-over} MIN LEFT"
                    else -> "ON TARGET"
                },
                style = Kaiju.type.eyebrow,
                color = if (over > 0) colors.signal else if (over == 0) colors.accent else colors.gold,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        val scale = maxOf(planned, target).coerceAtLeast(1)
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            drawRect(colors.surfaceInset)
            var x = 0f
            val gap = 2.dp.toPx()
            segments.forEachIndexed { i, m ->
                val w = size.width * m / scale
                drawRect(if (i % 2 == 0) colors.accent else colors.action, topLeft = Offset(x, 0f), size = Size((w - gap).coerceAtLeast(1f), size.height))
                x += w
            }
            val tx = size.width * target / scale
            drawRect(colors.gold, topLeft = Offset((tx - 1.5.dp.toPx()).coerceAtLeast(0f), -2f), size = Size(3.dp.toPx(), size.height + 4f))
        }
    }
}

@Composable
private fun SegmentEditorRow(
    index: Int,
    count: Int,
    segment: SegmentDraft,
    techniqueNames: List<String>,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LessonSegmentRow(
            index = index + 1,
            title = segment.title.ifBlank { "Untitled segment" },
            minutes = segment.minutes,
            kindLabel = segment.kind.takeIf { it != SegmentKind.OTHER }?.label,
            techniques = techniqueNames,
            notes = segment.notes,
            modifier = Modifier
                .weight(1f)
                .clickable(onClickLabel = "Edit segment", role = Role.Button, onClick = onEdit),
        )
        Column {
            KaijuIconButton(KaijuIcons.ArrowUp, "Move segment ${index + 1} up", { onMove(-1) }, enabled = index > 0, tint = Kaiju.colors.inkMuted)
            KaijuIconButton(KaijuIcons.ArrowDown, "Move segment ${index + 1} down", { onMove(1) }, enabled = index < count - 1, tint = Kaiju.colors.inkMuted)
        }
    }
}

@Composable
private fun SegmentSheet(
    segment: SegmentDraft,
    position: Int,
    techniques: List<TechniqueWithUsage>,
    onDone: (SegmentDraft) -> Unit,
    onDelete: () -> Unit,
    onCreateTechnique: suspend (String) -> String,
    onDismiss: () -> Unit,
) {
    val colors = Kaiju.colors
    var draft by remember(segment.id) { mutableStateOf(segment) }
    var newTechnique by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    KaijuBottomSheet(onDismiss = { onDone(draft) }, title = "Segment ${indexLabel(position)}", eyebrow = "Edit segment") {
        KaijuTextField(
            value = draft.title,
            onValueChange = { draft = draft.copy(title = it) },
            label = "Title",
            placeholder = "e.g. Partner drilling",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.s))
        NumberStepper("Minutes", draft.minutes, { draft = draft.copy(minutes = it) }, range = 1..180)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            listOf(5, 10, 15, 20, 30).forEach { m ->
                KaijuChip("$m min", selected = draft.minutes == m, onClick = { draft = draft.copy(minutes = m) })
            }
        }
        Spacer(Modifier.height(KaijuSpace.m))
        Text("Type", style = Kaiju.type.label, color = colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            SegmentKind.entries.forEach { k ->
                KaijuChip(k.label, selected = draft.kind == k, onClick = { draft = draft.copy(kind = k) })
            }
        }
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuTextField(
            value = draft.notes,
            onValueChange = { draft = draft.copy(notes = it) },
            label = "Notes",
            placeholder = "Cues, details, partner instructions",
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.m))
        Text("Techniques", style = Kaiju.type.label, color = colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        if (techniques.isEmpty()) {
            Text("Your technique library is empty. Add one below.", style = Kaiju.type.bodyS, color = colors.inkMuted)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            techniques.forEach { t ->
                val id = t.technique.id
                KaijuChip(
                    t.technique.name,
                    selected = id in draft.techniqueIds,
                    onClick = {
                        draft = draft.copy(techniqueIds = if (id in draft.techniqueIds) draft.techniqueIds - id else draft.techniqueIds + id)
                    },
                )
            }
        }
        Spacer(Modifier.height(KaijuSpace.s))
        Row(verticalAlignment = Alignment.Bottom) {
            KaijuTextField(
                value = newTechnique,
                onValueChange = { newTechnique = it },
                label = "New technique",
                placeholder = "e.g. Knee shield recovery",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.padding(start = KaijuSpace.xs))
            KaijuButton(
                "Add",
                {
                    val name = newTechnique.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            val id = onCreateTechnique(name)
                            draft = draft.copy(techniqueIds = draft.techniqueIds + id)
                            newTechnique = ""
                        }
                    }
                },
                variant = ButtonVariant.Secondary,
                enabled = newTechnique.isNotBlank(),
            )
        }
        Spacer(Modifier.height(KaijuSpace.l))
        KaijuButton("Done", { onDone(draft) }, Modifier.fillMaxWidth(), icon = KaijuIcons.Check)
        Spacer(Modifier.height(KaijuSpace.s))
        KaijuButton("Remove segment", onDelete, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
        Spacer(Modifier.height(KaijuSpace.xs))
        Text(
            "Changes apply to this plan. Classes already taught keep their own copy.",
            style = Kaiju.type.caption,
            color = colors.inkMuted,
        )
    }
}
