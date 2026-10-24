package com.earlthekaiju.coach.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.TechniqueWithUsage
import com.earlthekaiju.coach.designsystem.component.ConceptBanner
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.KaijuChip
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
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
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
) {
    val grouped: Map<String, List<TechniqueWithUsage>>
        get() = techniques
            .filter { query.isBlank() || it.technique.name.contains(query.trim(), true) || it.technique.category.contains(query.trim(), true) }
            .groupBy { it.technique.category.ifBlank { "Uncategorized" } }
}

class LibraryViewModel(c: AppContainer) : ViewModel() {
    private val media = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    val state: StateFlow<LibraryUiState> = combine(c.techniques.observeAll(), media, query) { t, m, q ->
        LibraryUiState(loading = false, showMedia = m, query = q, techniques = t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setShowMedia(show: Boolean) { media.value = show }
    fun setQuery(q: String) { query.value = q }
}

data class LibraryActions(
    val onShowMedia: (Boolean) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onOpenTechnique: (String) -> Unit = {},
    val onAddTechnique: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun LibraryDestination(onOpenTechnique: (String) -> Unit, onAddTechnique: () -> Unit, onOpenSettings: () -> Unit) {
    val vm = kaijuViewModel { c, _ -> LibraryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    LibraryScreen(state, LibraryActions(vm::setShowMedia, vm::setQuery, onOpenTechnique, onAddTechnique, onOpenSettings))
}

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
            item(key = "media") { MediaConcept(Modifier.readableWidth()) }
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

/**
 * Sprint 01 media library: a clearly labeled concept, not working software. It shows the
 * planned components and states with empty frames, never fake videos or fake playback.
 */
@Composable
private fun MediaConcept(modifier: Modifier) {
    val colors = Kaiju.colors
    Column(modifier) {
        ConceptBanner(
            title = "Concept · planned for a later sprint",
            body = "Nothing on this tab plays or stores video yet. It previews how your own technique videos will be organized.",
        )
        SectionHeader("Planned layout", index = 1, modifier = Modifier.padding(top = KaijuSpace.l))
        Spacer(Modifier.height(KaijuSpace.s))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            listOf("Guard retention", "Passing", "Kids").forEach { KaijuChip(it, selected = false, onClick = {}) }
        }
        Spacer(Modifier.height(KaijuSpace.s))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            ConceptTile("Video from this phone", "Local file reference", Modifier.weight(1f))
            ConceptTile("Linked to a lesson", "Opens from Class Mode", Modifier.weight(1f))
        }
        SectionHeader("What it will do", index = 2, modifier = Modifier.padding(top = KaijuSpace.l))
        listOf(
            "Reference videos you own that already live on the phone. Nothing is uploaded or copied.",
            "Tag by position and technique; attach clips to lesson segments.",
            "Play a clip from Class Mode, and later cast it to a TV where Android supports it.",
            "Large files stay out of desktop sync and get their own transfer step.",
        ).forEach { line ->
            Row(Modifier.padding(vertical = KaijuSpace.xs)) {
                Box(Modifier.padding(top = 9.dp).size(width = 10.dp, height = 2.dp).background(colors.gold))
                Spacer(Modifier.width(KaijuSpace.s))
                Text(line, style = Kaiju.type.body, color = colors.ink)
            }
        }
        Spacer(Modifier.height(KaijuSpace.xl))
    }
}

@Composable
private fun ConceptTile(title: String, caption: String, modifier: Modifier) {
    val colors = Kaiju.colors
    Column(
        modifier
            .border(1.dp, colors.line, KaijuShapes.Card)
            .padding(KaijuSpace.s)
            .semantics(mergeDescendants = true) { contentDescription = "Concept placeholder: $title, $caption" },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
            val dash = colors.lineStrong
            Canvas(Modifier.fillMaxSize()) {
                drawRect(dash, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                drawLine(dash, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = 1f)
                drawLine(dash, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1f)
            }
            Icon(KaijuIcons.Video, null, tint = colors.inkMuted, modifier = Modifier.size(28.dp).background(colors.background))
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Eyebrow("Concept", color = colors.gold)
        Text(title, style = Kaiju.type.label, color = colors.ink)
        Text(caption, style = Kaiju.type.caption, color = colors.inkMuted)
    }
}
