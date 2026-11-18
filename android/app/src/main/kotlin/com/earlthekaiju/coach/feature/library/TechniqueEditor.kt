package com.earlthekaiju.coach.feature.library

import androidx.compose.material3.Text
import com.earlthekaiju.coach.data.media.MediaRepository
import com.earlthekaiju.coach.data.media.MediaVideo
import com.earlthekaiju.coach.designsystem.component.KaijuListRow
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import com.earlthekaiju.coach.data.repo.TechniqueDraft
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.navigation.TechniqueEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TechniqueEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: TechniqueDraft = TechniqueDraft(),
    val categories: List<String> = emptyList(),
    val nameError: String? = null,
    val done: Boolean = false,
    /** Library videos that show this technique, on the whole video or a clip. */
    val videos: List<MediaVideo> = emptyList(),
)

class TechniqueEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val id = handle.toRoute<TechniqueEditorRoute>().techniqueId
    private val form = MutableStateFlow(TechniqueEditorUiState())

    private val videos = if (id == null) flowOf(emptyList()) else c.media.observeForTechnique(id)

    val state: StateFlow<TechniqueEditorUiState> = combine(form, c.techniques.observeAll(), videos) { f, all, v ->
        f.copy(categories = all.map { it.technique.category }.filter { it.isNotBlank() }.distinct().sorted(), videos = v)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        viewModelScope.launch {
            val t = id?.let { c.techniques.get(it) }
            form.value = form.value.copy(
                loading = false,
                isNew = t == null,
                draft = t?.let { TechniqueDraft(it.id, it.name, it.category, it.notes) } ?: TechniqueDraft(),
            )
        }
    }

    fun update(transform: (TechniqueDraft) -> TechniqueDraft) {
        form.value = form.value.copy(draft = transform(form.value.draft), nameError = null)
    }

    fun save() = viewModelScope.launch {
        if (form.value.draft.name.isBlank()) {
            form.value = form.value.copy(nameError = "Name the technique.")
            return@launch
        }
        c.techniques.save(form.value.draft)
        form.value = form.value.copy(done = true)
    }

    fun delete() = viewModelScope.launch {
        form.value.draft.id?.let { c.techniques.delete(it) }
        form.value = form.value.copy(done = true)
    }
}

@Composable
fun TechniqueEditorDestination(onClose: () -> Unit, onOpenVideo: (mediaId: String, clipId: String?) -> Unit = { _, _ -> }) {
    val vm = kaijuViewModel { c, h -> TechniqueEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    TechniqueEditorScreen(state, onClose, vm::update, { vm.save() }, { vm.delete() }, onOpenVideo)
}

@Composable
fun TechniqueEditorScreen(
    state: TechniqueEditorUiState,
    onClose: () -> Unit,
    onUpdate: ((TechniqueDraft) -> TechniqueDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenVideo: (mediaId: String, clipId: String?) -> Unit = { _, _ -> },
) {
    val colors = Kaiju.colors
    var confirmDelete by remember { mutableStateOf(false) }
    val d = state.draft
    // imePadding on the whole screen: with the keyboard up the scroll area shrinks (keeping the
    // focused field in view) and the bottom action bar sits right above the keyboard.
    Column(Modifier.fillMaxSize().background(colors.background).imePadding()) {
        KaijuTopBar(
            title = if (state.isNew) "Add technique" else d.name.ifBlank { "Technique" },
            eyebrow = "Library",
            onBack = onClose,
            actions = { KaijuButton("Save", onSave, variant = ButtonVariant.Ghost, size = ButtonSize.Compact) },
        )
        if (state.loading) {
            LoadingState("Loading technique", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth().padding(top = KaijuSpace.m)) {
                KaijuTextField(
                    d.name, { v -> onUpdate { it.copy(name = v) } }, "Name",
                    placeholder = "e.g. Knee cut pass",
                    errorText = state.nameError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(KaijuSpace.m))
                KaijuTextField(
                    d.category, { v -> onUpdate { it.copy(category = v) } }, "Category",
                    placeholder = "e.g. Passing",
                    supportingText = "Positions or themes — whatever matches how you teach.",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                if (state.categories.isNotEmpty()) {
                    Spacer(Modifier.height(KaijuSpace.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                        state.categories.forEach { cat -> KaijuChip(cat, d.category == cat, { onUpdate { it.copy(category = cat) } }) }
                    }
                }
                Spacer(Modifier.height(KaijuSpace.m))
                KaijuTextField(
                    d.notes, { v -> onUpdate { it.copy(notes = v) } }, "Notes",
                    placeholder = "Key details, common mistakes, setups",
                    singleLine = false,
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                if (!state.isNew && d.id != null) {
                    TechniqueVideos(d.id, state.videos, onOpenVideo)
                }
                if (!state.isNew) {
                    Spacer(Modifier.height(KaijuSpace.l))
                    KaijuButton("Delete technique", { confirmDelete = true }, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
                }
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
        Column(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KaijuButton(if (state.isNew) "Add to library" else "Save changes", onSave, Modifier.readableWidth(), icon = KaijuIcons.Check)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete technique?",
            body = "It's removed from the library and from lesson plans. Classes already taught keep it in their record.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** "Videos" on a technique's page: each video, and the clips inside it that show this technique. */
@Composable
private fun TechniqueVideos(techniqueId: String, videos: List<MediaVideo>, onOpen: (String, String?) -> Unit) {
    val colors = Kaiju.colors
    SectionHeader(if (videos.isEmpty()) "Videos" else "Videos · ${videos.size}", modifier = Modifier.padding(top = KaijuSpace.l))
    if (videos.isEmpty()) {
        Text(
            "No videos yet. In Library › Media, label a video or a clip with this technique and it shows up here.",
            style = Kaiju.type.bodyS,
            color = colors.inkMuted,
        )
        return
    }
    videos.forEach { v ->
        val clips = v.clips.filter { c -> c.techniques.any { it.id == techniqueId } }
        val wholeVideo = v.techniques.any { it.id == techniqueId }
        KaijuListRow(
            title = v.item.title,
            subtitle = if (wholeVideo || clips.isEmpty()) "Whole video" else null,
            icon = KaijuIcons.Video,
            value = if (v.item.durationMs > 0) MediaRepository.clockLabel(v.item.durationMs) else null,
            onClick = { onOpen(v.id, null) },
        )
        clips.forEach { c ->
            KaijuListRow(
                title = c.clip.title,
                subtitle = "Clip · ${MediaRepository.clockLabel(c.clip.startMs)}–${MediaRepository.clockLabel(c.clip.endMs)}",
                icon = KaijuIcons.Clip,
                onClick = { onOpen(v.id, c.id) },
                modifier = Modifier.padding(start = KaijuSpace.m),
            )
        }
    }
}
