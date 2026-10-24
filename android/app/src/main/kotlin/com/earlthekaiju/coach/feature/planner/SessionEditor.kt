package com.earlthekaiju.coach.feature.planner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.LessonPlanSummary
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.repo.SessionDraft
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuDatePickerDialog
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTimePickerDialog
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.NumberStepper
import com.earlthekaiju.coach.designsystem.component.OptionSheet
import com.earlthekaiju.coach.designsystem.component.PickerField
import com.earlthekaiju.coach.designsystem.component.PickerOption
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.navigation.SessionEditorRoute
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalUse24Hour
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SessionEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: SessionDraft,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val programs: List<ProgramEntity> = emptyList(),
    val lessons: List<LessonPlanSummary> = emptyList(),
    /** Set after a successful save/delete so the screen can leave. */
    val done: Boolean = false,
    /** The draft as loaded; leaving with a different draft asks first. */
    val original: SessionDraft? = null,
) {
    val dirty: Boolean get() = original != null && draft != original
    val programName: String? get() = programs.firstOrNull { it.id == draft.programId }?.name
    val lessonTitle: String? get() = lessons.firstOrNull { it.plan.id == draft.lessonPlanId }?.plan?.title
    val titlePlaceholder: String get() = lessonTitle ?: programName ?: "e.g. Fundamentals"
}

class SessionEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<SessionEditorRoute>()
    private val today = c.clock.today()
    private val form = MutableStateFlow(
        SessionEditorUiState(draft = SessionDraft(date = route.date?.let(LocalDate::parse) ?: today, lessonPlanId = route.lessonPlanId)),
    )

    val state: StateFlow<SessionEditorUiState> =
        combine(form, c.programs.observeAll(), c.lessons.observeTemplates()) { f, programs, lessons ->
            f.copy(programs = programs, lessons = lessons)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        viewModelScope.launch {
            val id = route.sessionId
            if (id != null) {
                val s = c.sessions.get(id)
                if (s != null) {
                    val loaded = SessionDraft(
                        id = s.id, title = s.title, programId = s.programId, lessonPlanId = s.lessonPlanId,
                        date = s.date, startTime = s.startTime, durationMinutes = s.durationMinutes, location = s.location,
                    )
                    form.value = form.value.copy(loading = false, isNew = false, status = s.status, draft = loaded, original = loaded)
                    return@launch
                }
            }
            // New class from a lesson plan: borrow its program and length.
            val planId = route.lessonPlanId
            val plan = planId?.let { pid -> c.lessons.observeTemplates().first().firstOrNull { it.plan.id == pid } }
            val initial = form.value.draft.copy(
                programId = plan?.plan?.programId,
                durationMinutes = plan?.plan?.targetMinutes ?: 60,
            )
            form.value = form.value.copy(loading = false, draft = initial, original = initial)
        }
    }

    fun update(transform: (SessionDraft) -> SessionDraft) {
        form.value = form.value.copy(draft = transform(form.value.draft))
    }

    fun selectLesson(planId: String?) {
        val plan = state.value.lessons.firstOrNull { it.plan.id == planId }
        update { d ->
            d.copy(
                lessonPlanId = planId,
                programId = d.programId ?: plan?.plan?.programId,
                durationMinutes = if (form.value.isNew && plan != null) plan.plan.targetMinutes else d.durationMinutes,
            )
        }
    }

    fun save() = viewModelScope.launch {
        c.sessions.save(form.value.draft)
        form.value = form.value.copy(done = true)
    }

    fun setCanceled(canceled: Boolean) = viewModelScope.launch {
        val id = form.value.draft.id ?: return@launch
        c.sessions.save(form.value.draft)
        c.sessions.setStatus(id, if (canceled) SessionStatus.CANCELED else SessionStatus.SCHEDULED)
        form.value = form.value.copy(done = true)
    }

    fun delete() = viewModelScope.launch {
        form.value.draft.id?.let { c.sessions.delete(it) }
        form.value = form.value.copy(done = true)
    }
}

data class SessionEditorActions(
    val onBack: () -> Unit = {},
    val onUpdate: ((SessionDraft) -> SessionDraft) -> Unit = {},
    val onSelectLesson: (String?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onSetCanceled: (Boolean) -> Unit = {},
    val onDelete: () -> Unit = {},
    val onManagePrograms: () -> Unit = {},
)

@Composable
fun SessionEditorDestination(onDone: () -> Unit, onManagePrograms: () -> Unit) {
    val vm = kaijuViewModel { c, h -> SessionEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    SessionEditorScreen(
        state,
        SessionEditorActions(
            onBack = onDone,
            onUpdate = vm::update,
            onSelectLesson = vm::selectLesson,
            onSave = { vm.save() },
            onSetCanceled = { vm.setCanceled(it) },
            onDelete = { vm.delete() },
            onManagePrograms = onManagePrograms,
        ),
    )
}

private enum class EditorDialog { None, Date, Time, Program, Lesson, Delete, Cancel, Discard }

@Composable
fun SessionEditorScreen(state: SessionEditorUiState, actions: SessionEditorActions) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    var dialog by remember { mutableStateOf(EditorDialog.None) }
    val d = state.draft
    // Same rule as the lesson and student editors: unsaved changes are never lost silently.
    val back = { if (state.dirty) dialog = EditorDialog.Discard else actions.onBack() }
    BackHandler(enabled = state.dirty) { dialog = EditorDialog.Discard }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = if (state.isNew) "Schedule class" else "Edit class",
            eyebrow = Format.dayEyebrow(d.date),
            onBack = back,
            backDescription = "Close without saving",
        )
        if (state.loading) {
            LoadingState("Loading class", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth()) {
                SectionHeader("When", index = 1, modifier = Modifier.padding(top = KaijuSpace.m))
                Spacer(Modifier.height(KaijuSpace.s))
                Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
                    PickerField("Date", Format.shortDate(d.date), { dialog = EditorDialog.Date }, Modifier.weight(1.2f), icon = KaijuIcons.Planner)
                    PickerField("Start", Format.time(d.startTime, use24h), { dialog = EditorDialog.Time }, Modifier.weight(1f), icon = KaijuIcons.Clock)
                }
                Spacer(Modifier.height(KaijuSpace.s))
                NumberStepper(
                    label = "Length",
                    value = d.durationMinutes,
                    onValueChange = { v -> actions.onUpdate { it.copy(durationMinutes = v) } },
                    range = 15..240,
                    step = 5,
                    valueText = { "$it" },
                    unitLabel = "minutes",
                )

                SectionHeader("What", index = 2, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                PickerField("Program", state.programName ?: "", { dialog = EditorDialog.Program }, placeholder = "No program (no expected roster)")
                Spacer(Modifier.height(KaijuSpace.m))
                PickerField("Lesson plan", state.lessonTitle ?: "", { dialog = EditorDialog.Lesson }, placeholder = "No lesson plan yet")
                Spacer(Modifier.height(KaijuSpace.m))
                KaijuTextField(
                    value = d.title,
                    onValueChange = { v -> actions.onUpdate { it.copy(title = v) } },
                    label = "Class name",
                    placeholder = state.titlePlaceholder,
                    supportingText = "Optional. Shown on Today and in the planner.",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                SectionHeader("Where", index = 3, modifier = Modifier.padding(top = KaijuSpace.l))
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuTextField(
                    value = d.location,
                    onValueChange = { v -> actions.onUpdate { it.copy(location = v) } },
                    label = "Location",
                    placeholder = "e.g. Main mat",
                    leadingIcon = KaijuIcons.Location,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                if (!state.isNew) {
                    SectionHeader("Manage", index = 4, modifier = Modifier.padding(top = KaijuSpace.l))
                    Spacer(Modifier.height(KaijuSpace.s))
                    if (state.status == SessionStatus.CANCELED) {
                        KaijuButton("Restore class", { actions.onSetCanceled(false) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Undo)
                    } else if (state.status == SessionStatus.SCHEDULED) {
                        KaijuButton("Cancel this class", { dialog = EditorDialog.Cancel }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Close)
                    }
                    Spacer(Modifier.height(KaijuSpace.s))
                    KaijuButton("Delete class", { dialog = EditorDialog.Delete }, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KaijuButton(if (state.isNew) "Schedule class" else "Save changes", actions.onSave, Modifier.readableWidth(), icon = KaijuIcons.Check)
        }
    }

    when (dialog) {
        EditorDialog.Date -> KaijuDatePickerDialog(d.date, { date -> actions.onUpdate { it.copy(date = date) }; dialog = EditorDialog.None }, { dialog = EditorDialog.None })
        EditorDialog.Time -> KaijuTimePickerDialog(d.startTime, use24h, { t -> actions.onUpdate { it.copy(startTime = t) }; dialog = EditorDialog.None }, { dialog = EditorDialog.None })
        EditorDialog.Program -> OptionSheet(
            title = "Program",
            options = listOf(PickerOption(null, "No program", "No expected roster; mark drop-ins by hand")) +
                state.programs.map { PickerOption(it.id, it.name, it.audience.label) },
            selectedId = d.programId,
            onSelect = { id -> actions.onUpdate { it.copy(programId = id) }; dialog = EditorDialog.None },
            onDismiss = { dialog = EditorDialog.None },
            footer = { KaijuButton("Manage programs", { dialog = EditorDialog.None; actions.onManagePrograms() }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary) },
        )
        EditorDialog.Lesson -> OptionSheet(
            title = "Lesson plan",
            options = listOf(PickerOption(null, "No lesson plan", "Add one later")) +
                state.lessons.map { PickerOption(it.plan.id, it.plan.title, "${it.programName ?: "Any program"} · ${Format.count(it.segmentCount, "segment")} · ${it.segmentMinutes} min") },
            selectedId = d.lessonPlanId,
            onSelect = { id -> actions.onSelectLesson(id); dialog = EditorDialog.None },
            onDismiss = { dialog = EditorDialog.None },
        )
        EditorDialog.Cancel -> ConfirmDialog(
            title = "Cancel this class?",
            body = "It stays on the planner, struck through, and you can restore it. Attendance isn't taken for canceled classes.",
            confirmLabel = "Cancel class",
            dismissLabel = "Keep it",
            onConfirm = { dialog = EditorDialog.None; actions.onSetCanceled(true) },
            onDismiss = { dialog = EditorDialog.None },
            destructive = true,
        )
        EditorDialog.Delete -> ConfirmDialog(
            title = "Delete this class?",
            body = "The class, its attendance and its notes are removed from the planner. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { dialog = EditorDialog.None; actions.onDelete() },
            onDismiss = { dialog = EditorDialog.None },
            destructive = true,
        )
        EditorDialog.Discard -> ConfirmDialog(
            title = "Discard changes?",
            body = if (state.isNew) "This class won't be scheduled." else "Your edits to this class won't be saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = { dialog = EditorDialog.None; actions.onBack() },
            onDismiss = { dialog = EditorDialog.None },
            destructive = true,
        )
        EditorDialog.None -> Unit
    }
}
