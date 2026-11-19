package com.earlthekaiju.coach.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.earlthekaiju.coach.domain.model.MediaKind
import com.earlthekaiju.coach.feature.media.MediaTabActions
import com.earlthekaiju.coach.feature.media.MediaTabState
import com.earlthekaiju.coach.feature.media.mediaTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.TechniqueWithUsage
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.ScreenHeader
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentedControl
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.TechniqueCard
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val loading: Boolean = true,
    val showMedia: Boolean = false,
    val query: String = "",
    val techniques: List<TechniqueWithUsage> = emptyList(),
    val media: MediaTabState = MediaTabState(),
) {
    val grouped: Map<String, List<TechniqueWithUsage>>
        get() = techniques
            .filter { query.isBlank() || it.technique.name.contains(query.trim(), true) || it.technique.category.contains(query.trim(), true) }
            .groupBy { it.technique.category.ifBlank { "Uncategorized" } }
}

class LibraryViewModel(private val c: AppContainer) : ViewModel() {
    private val showMedia = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val mediaQuery = MutableStateFlow("")
    private val mediaKind = MutableStateFlow<MediaKind?>(null)

    private val usedBytes = c.media.observeLibrary()
        .map { c.media.files.usedBytes() }
        .flowOn(Dispatchers.IO)

    private val mediaState = combine(c.media.observeLibrary(), mediaQuery, mediaKind, c.mediaTransfers.state, usedBytes) { v, q, k, t, b ->
        MediaTabState(videos = v, query = q, kind = k, transfer = t, usedBytes = b, thumbDir = c.media.files.thumbDir)
    }

    val state: StateFlow<LibraryUiState> = combine(c.techniques.observeAll(), showMedia, query, mediaState) { t, m, q, media ->
        LibraryUiState(loading = false, showMedia = m, query = q, techniques = t, media = media)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setShowMedia(show: Boolean) { showMedia.value = show }
    fun setQuery(q: String) { query.value = q }
    fun setMediaQuery(q: String) { mediaQuery.value = q }
    fun setMediaKind(k: MediaKind?) { mediaKind.value = k }
    fun import(uris: List<Uri>) = c.mediaTransfers.import(uris)
    fun exportAll(tree: Uri) = c.mediaTransfers.exportAll(tree)
    fun dismissTransfer() = c.mediaTransfers.dismissResult()
}

data class LibraryActions(
    val onShowMedia: (Boolean) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onOpenTechnique: (String) -> Unit = {},
    val onAddTechnique: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val media: MediaTabActions = MediaTabActions(),
)

@Composable
fun LibraryDestination(
    onOpenTechnique: (String) -> Unit,
    onAddTechnique: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    val vm = kaijuViewModel { c, _ -> LibraryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Android's photo picker, videos only: no storage permission needed. Picked videos are copied
    // into the app's private folder.
    val pickVideos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_IMPORT)) { uris ->
        vm.import(uris)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        tree?.let(vm::exportAll)
    }
    LibraryScreen(
        state,
        LibraryActions(
            vm::setShowMedia, vm::setQuery, onOpenTechnique, onAddTechnique, onOpenSettings,
            MediaTabActions(
                onImport = { pickVideos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                onExportAll = { pickFolder.launch(null) },
                onQuery = vm::setMediaQuery,
                onKind = vm::setMediaKind,
                onOpenVideo = onOpenVideo,
                onDismissTransfer = vm::dismissTransfer,
            ),
        ),
    )
}

private const val MAX_IMPORT = 20

@Composable
fun LibraryScreen(state: LibraryUiState, actions: LibraryActions) {
    val colors = Kaiju.colors
    LazyColumn(
        Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = KaijuSpace.gutter, vertical = KaijuSpace.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Library",
                eyebrow = "Techniques & media",
                modifier = Modifier.readableWidth().windowInsetsPadding(WindowInsets.statusBars),
                trailing = { KaijuIconButton(KaijuIcons.Settings, "Settings", actions.onOpenSettings, tint = colors.inkMuted) },
            )
        }
        item(key = "mode") {
            SegmentedControl(
                listOf("Techniques", "Media"),
                if (state.showMedia) 1 else 0,
                { actions.onShowMedia(it == 1) },
                Modifier.readableWidth().padding(top = KaijuSpace.s, bottom = KaijuSpace.m),
            )
        }
        if (state.loading) {
            item { LoadingState("Loading library", Modifier.readableWidth()) }
            return@LazyColumn
        }
        if (state.showMedia) {
            mediaTab(state.media, actions.media)
            return@LazyColumn
        }
        if (state.techniques.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    eyebrow = "Technique library",
                    title = "No techniques yet",
                    body = "Add the techniques you teach, then attach them to lesson segments. They also appear in Class Mode.",
                    primary = StateAction("Add technique", actions.onAddTechnique),
                    modifier = Modifier.readableWidth(),
                )
            }
            return@LazyColumn
        }
        item(key = "search") {
            Row(Modifier.readableWidth(), verticalAlignment = Alignment.Bottom) {
                KaijuTextField(
                    state.query, actions.onQuery, "Search techniques",
                    Modifier.weight(1f),
                    placeholder = "Name or category",
                    leadingIcon = KaijuIcons.Search,
                )
                Spacer(Modifier.width(KaijuSpace.s))
                KaijuIconButton(KaijuIcons.Add, "Add technique", actions.onAddTechnique, filled = true, size = 52.dp)
            }
        }
        var index = 0
        state.grouped.forEach { (category, list) ->
            index++
            val i = index
            item(key = "cat-$category") {
                SectionHeader("$category · ${list.size}", index = i, modifier = Modifier.readableWidth().padding(top = KaijuSpace.l))
            }
            items(list, key = { it.technique.id }) { t ->
                TechniqueCard(
                    name = t.technique.name,
                    category = "",
                    notes = t.technique.notes,
                    usedInCount = t.usageCount,
                    onClick = { actions.onOpenTechnique(t.technique.id) },
                    modifier = Modifier.readableWidth().padding(top = KaijuSpace.xs),
                )
            }
        }
        item { Spacer(Modifier.height(KaijuSpace.xl)) }
    }
}

