package com.earlthekaiju.coach.feature.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.earlthekaiju.coach.data.db.MediaItemEntity
import com.earlthekaiju.coach.data.media.ClipDraft
import com.earlthekaiju.coach.data.media.MediaClip
import com.earlthekaiju.coach.data.media.MediaDetailsDraft
import com.earlthekaiju.coach.data.media.MediaRepository.Companion.clockLabel
import com.earlthekaiju.coach.data.media.MediaVideo
import com.earlthekaiju.coach.data.media.TechniqueRef
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuCard
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuSwitchRow
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.TechniqueTag
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.MediaKind
import com.earlthekaiju.coach.navigation.VideoRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Common BJJ positions offered as one-tap labels; anything else can be typed. */
val POSITION_SUGGESTIONS = listOf(
    "Standing", "Closed guard", "Open guard", "Half guard", "Side control",
    "Mount", "Back", "Turtle", "North-south", "Leg entanglements",
)

data class VideoUiState(
    val loading: Boolean = true,
    val video: MediaVideo? = null,
    val allTechniques: List<TechniqueRef> = emptyList(),
    val positions: List<String> = POSITION_SUGGESTIONS,
    val playingClipId: String? = null,
    val markInMs: Long? = null,
    val markOutMs: Long? = null,
    val deleted: Boolean = false,
) {
    val playingClip: MediaClip? get() = video?.clips?.firstOrNull { it.id == playingClipId }
}

class VideoViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val route = handle.toRoute<VideoRoute>()
    private val local = MutableStateFlow(VideoUiState(playingClipId = route.clipId))

    val state: StateFlow<VideoUiState> = combine(
        local,
        c.media.observeLibrary(),
        c.techniques.observeAll(),
    ) { l, library, techniques ->
        l.copy(
            loading = false,
            video = library.firstOrNull { it.id == route.mediaId },
            allTechniques = techniques.map { TechniqueRef(it.technique.id, it.technique.name) },
            positions = (POSITION_SUGGESTIONS + library.map { it.item.position }.filter { it.isNotBlank() }).distinctBy { it.lowercase() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    fun file(item: MediaItemEntity): File = c.media.files.video(item)

    fun playClip(id: String?) { local.value = local.value.copy(playingClipId = id) }
    fun markIn(ms: Long) { local.value = local.value.copy(markInMs = ms) }
    fun markOut(ms: Long) { local.value = local.value.copy(markOutMs = ms) }
    fun clearMarks() { local.value = local.value.copy(markInMs = null, markOutMs = null) }

    fun saveDetails(draft: MediaDetailsDraft) = viewModelScope.launch { c.media.saveDetails(draft) }

    fun saveClip(draft: ClipDraft) = viewModelScope.launch {
        c.media.saveClip(draft)
        if (draft.id == null) clearMarks()
    }

    fun deleteClip(id: String) = viewModelScope.launch {
        if (local.value.playingClipId == id) playClip(null)
        c.media.deleteClip(id)
    }

    fun delete() = viewModelScope.launch {
        c.media.delete(route.mediaId)
        local.value = local.value.copy(deleted = true)
    }
}

/** Everything the controls show and do, so screenshots can draw them without a real player. */
data class PlayerControls(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val absolutePositionMs: Long = 0L,
    val speed: Float = 1f,
    val loop: Boolean = false,
    val mirror: Boolean = false,
    val sound: Boolean = true,
    val onToggle: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onSpeed: () -> Unit = {},
    val onLoop: () -> Unit = {},
    val onMirror: () -> Unit = {},
    val onSound: () -> Unit = {},
)

data class VideoActions(
    val onBack: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onPlayClip: (String?) -> Unit = {},
    val onMarkIn: () -> Unit = {},
    val onMarkOut: () -> Unit = {},
    val onClearMarks: () -> Unit = {},
    val onSaveDetails: (MediaDetailsDraft) -> Unit = {},
    val onSaveClip: (ClipDraft) -> Unit = {},
    val onDeleteClip: (String) -> Unit = {},
    val onDelete: () -> Unit = {},
)

@Composable
fun VideoDestination(onBack: () -> Unit) {
    val vm = kaijuViewModel { c, h -> VideoViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val player = rememberKaijuPlayer()
    val video = state.video
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(video?.item?.fileName, state.playingClip?.clip) {
        if (video != null) player.load(vm.file(video.item), state.playingClip?.clip)
    }
    VideoScreen(
        state,
        PlayerControls(
            isPlaying = player.isPlaying,
            positionMs = player.positionMs,
            durationMs = player.durationMs,
            absolutePositionMs = player.absolutePositionMs,
            speed = player.speed,
            loop = player.loop,
            mirror = player.mirror,
            sound = player.sound,
            onToggle = player::toggle,
            onSeek = player::seekTo,
            onSpeed = player::cycleSpeed,
            onLoop = player::toggleLoop,
            onMirror = player::toggleMirror,
            onSound = player::toggleSound,
        ),
        VideoActions(
            onBack = onBack,
            onShare = { video?.let { shareVideo(context, vm.file(it.item), it.item) } },
            onPlayClip = vm::playClip,
            onMarkIn = { vm.markIn(player.absolutePositionMs) },
            onMarkOut = { vm.markOut(player.absolutePositionMs) },
            onClearMarks = vm::clearMarks,
            onSaveDetails = { vm.saveDetails(it) },
            onSaveClip = { vm.saveClip(it) },
            onDeleteClip = { vm.deleteClip(it) },
            onDelete = { vm.delete() },
        ),
        surface = { m -> KaijuPlayerSurface(player, m) },
    )
}

/** Hands one video to the app the instructor picks (YouTube, Drive, Messages…) via the share sheet. */
private fun shareVideo(context: Context, file: File, item: MediaItemEntity) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(item.mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TITLE, item.title)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(send, "Share video"))
    } catch (_: ActivityNotFoundException) {
        // Nothing on the phone can receive a video; the sheet simply doesn't open.
    }
}

@Composable
fun VideoScreen(
    state: VideoUiState,
    controls: PlayerControls,
    actions: VideoActions,
    surface: @Composable (Modifier) -> Unit,
) {
    val colors = Kaiju.colors
    val video = state.video
    var editDetails by rememberSaveable { mutableStateOf(false) }
    var clipSheet by remember { mutableStateOf<ClipDraft?>(null) }
    var confirmShare by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteClip by remember { mutableStateOf<MediaClip?>(null) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(
            title = video?.item?.title ?: "Video",
            eyebrow = "Media",
            onBack = actions.onBack,
            actions = {
                if (video != null) {
                    KaijuIconButton(KaijuIcons.Share, "Share video", {
                        if (video.item.showsStudents || video.item.showsMinors) confirmShare = true else actions.onShare()
                    })
                    KaijuIconButton(KaijuIcons.Edit, "Edit labels", { editDetails = true })
                }
            },
        )
        if (state.loading) {
            LoadingState("Loading video", Modifier.padding(KaijuSpace.gutter))
            return@Column
        }
        if (video == null) {
            EmptyState(
                title = "This video isn't in the library",
                body = "It may have been deleted.",
                primary = StateAction("Back to library", actions.onBack),
                modifier = Modifier.padding(KaijuSpace.gutter),
            )
            return@Column
        }
        val item = video.item
        val clip = state.playingClip
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth().padding(top = KaijuSpace.s)) {
                // Picture: the video's own shape, between 3:4 (portrait phone video) and 16:9.
                val aspect = if (item.width > 0 && item.height > 0) (item.width.toFloat() / item.height).coerceIn(0.75f, 16f / 9f) else 16f / 9f
                Box(
                    Modifier.fillMaxWidth().aspectRatio(aspect).background(colors.surfaceInset)
                        .semantics { contentDescription = "Video: ${item.title}" },
                ) { surface(Modifier.fillMaxSize()) }

                if (clip != null) {
                    Spacer(Modifier.height(KaijuSpace.s))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("Playing clip")
                            Text(
                                "${clip.clip.title} · ${clockLabel(clip.clip.startMs)}–${clockLabel(clip.clip.endMs)}",
                                style = Kaiju.type.label, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        KaijuButton("Whole video", { actions.onPlayClip(null) }, variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
                    }
                }

                PlayerBar(controls)

                if (clip == null) {
                    ClipMarker(state, controls, actions) { start, end ->
                        clipSheet = ClipDraft(mediaId = item.id, startMs = start, endMs = end)
                    }
                }

                SectionHeader(
                    if (video.clips.isEmpty()) "Clips" else "Clips · ${video.clips.size}",
                    index = 1,
                    modifier = Modifier.padding(top = KaijuSpace.l),
                )
                if (video.clips.isEmpty()) {
                    Text(
                        "Mark the start and end of a move while the video plays, then save it as a clip. " +
                            "Clips are bookmarks: no copies, no extra space.",
                        style = Kaiju.type.bodyS,
                        color = colors.inkMuted,
                    )
                }
                video.clips.forEach { c ->
                    ClipRow(
                        c,
                        playing = c.id == state.playingClipId,
                        onPlay = { actions.onPlayClip(c.id) },
                        onEdit = {
                            clipSheet = ClipDraft(
                                c.id, item.id, c.clip.title, c.clip.startMs, c.clip.endMs, c.clip.notes, c.techniques.map { it.id }.toSet(),
                            )
                        },
                        onDelete = { confirmDeleteClip = c },
                        modifier = Modifier.padding(top = KaijuSpace.xs),
                    )
                }

                SectionHeader("Labels", index = 2, modifier = Modifier.padding(top = KaijuSpace.l))
                LabelsSummary(video) { editDetails = true }

                Spacer(Modifier.height(KaijuSpace.l))
                KaijuButton(
                    "Delete video", { confirmDelete = true }, Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Danger, icon = KaijuIcons.Delete,
                )
                Spacer(Modifier.height(KaijuSpace.xl))
            }
        }
    }

    if (editDetails && video != null) {
        DetailsSheet(video, state.allTechniques, state.positions, onDismiss = { editDetails = false }) {
            actions.onSaveDetails(it)
            editDetails = false
        }
    }
    clipSheet?.let { draft ->
        ClipSheet(draft, state.allTechniques, onDismiss = { clipSheet = null }) {
            actions.onSaveClip(it)
            clipSheet = null
        }
    }
    if (confirmShare && video != null) {
        ConfirmDialog(
            title = "Share a video with students in it?",
            body = if (video.item.showsMinors) {
                "This video is marked as showing minors. Only share it where their parents have agreed, and never publicly without permission."
            } else {
                "This video is marked as showing students. Make sure they're OK with where it's going."
            },
            confirmLabel = "Share",
            onConfirm = { confirmShare = false; actions.onShare() },
            onDismiss = { confirmShare = false },
        )
    }
    if (confirmDelete && video != null) {
        ConfirmDialog(
            title = "Delete this video?",
            body = "The file is removed from this phone along with its clips and labels. Copies you've shared or saved to a folder aren't affected.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
    confirmDeleteClip?.let { c ->
        ConfirmDialog(
            title = "Delete clip?",
            body = "“${c.clip.title}” is removed. The video itself stays.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { confirmDeleteClip = null; actions.onDeleteClip(c.id) },
            onDismiss = { confirmDeleteClip = null },
        )
    }
}

@Composable
private fun PlayerBar(c: PlayerControls) {
    val colors = Kaiju.colors
    Column(Modifier.fillMaxWidth().padding(top = KaijuSpace.s)) {
        val max = c.durationMs.coerceAtLeast(1L).toFloat()
        Slider(
            value = c.positionMs.coerceIn(0L, c.durationMs.coerceAtLeast(0L)).toFloat(),
            onValueChange = { c.onSeek(it.toLong()) },
            valueRange = 0f..max,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Position" },
            colors = SliderDefaults.colors(
                thumbColor = colors.action,
                activeTrackColor = colors.action,
                inactiveTrackColor = colors.surfaceInset,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            KaijuIconButton(
                if (c.isPlaying) KaijuIcons.Pause else KaijuIcons.Play,
                if (c.isPlaying) "Pause" else "Play",
                c.onToggle,
                filled = true,
                size = 56.dp,
            )
            Spacer(Modifier.width(KaijuSpace.s))
            Text(
                "${clockLabel(c.positionMs)} / ${clockLabel(c.durationMs)}",
                style = Kaiju.type.numeralM,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            ToggleChip("Loop", c.loop, c.onLoop, KaijuIcons.Loop)
            KaijuChip(
                speedLabel(c.speed),
                selected = c.speed != 1f,
                onClick = c.onSpeed,
                modifier = Modifier.semantics { stateDescription = "Speed ${speedLabel(c.speed)}" },
                icon = KaijuIcons.Speed,
            )
            ToggleChip("Mirror", c.mirror, c.onMirror, KaijuIcons.Mirror)
            ToggleChip("Sound", c.sound, c.onSound, if (c.sound) KaijuIcons.SoundOn else KaijuIcons.SoundOff)
        }
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    KaijuChip(label, selected = on, onClick = onClick, icon = icon)
}

private fun speedLabel(speed: Float): String = when (speed) {
    0.5f -> "½×"
    0.25f -> "¼×"
    else -> "1×"
}

@Composable
private fun ClipMarker(state: VideoUiState, c: PlayerControls, actions: VideoActions, onSave: (Long, Long) -> Unit) {
    val colors = Kaiju.colors
    val markIn = state.markInMs
    val markOut = state.markOutMs
    Column(Modifier.fillMaxWidth().padding(top = KaijuSpace.m)) {
        Eyebrow("Make a clip")
        Spacer(Modifier.height(KaijuSpace.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            KaijuButton(
                if (markIn == null) "Start here" else "Start ${clockLabel(markIn)}",
                actions.onMarkIn,
                Modifier.weight(1f),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Compact,
            )
            KaijuButton(
                if (markOut == null) "End here" else "End ${clockLabel(markOut)}",
                actions.onMarkOut,
                Modifier.weight(1f),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Compact,
            )
        }
        if (markIn != null && markOut != null) {
            Spacer(Modifier.height(KaijuSpace.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                KaijuButton("Clear", actions.onClearMarks, Modifier.weight(1f), variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
                KaijuButton("Save clip", { onSave(markIn, markOut) }, Modifier.weight(1f), size = ButtonSize.Compact, icon = KaijuIcons.Clip)
            }
        } else {
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text(
                "Play or drag to where a move starts and tap Start here, then where it ends and tap End here.",
                style = Kaiju.type.caption,
                color = colors.inkMuted,
            )
        }
    }
}

@Composable
private fun ClipRow(
    c: MediaClip,
    playing: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kaiju.colors
    KaijuCard(modifier, onClick = onPlay, onClickLabel = "Play clip", accentEdge = if (playing) colors.action else null, contentPadding = KaijuSpace.s) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.clip.title, style = Kaiju.type.label, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${clockLabel(c.clip.startMs)}–${clockLabel(c.clip.endMs)} · ${clockLabel(c.clip.endMs - c.clip.startMs)}",
                    style = Kaiju.type.caption,
                    color = colors.inkMuted,
                )
                if (c.techniques.isNotEmpty()) {
                    Text(c.techniques.joinToString(" · ") { it.name }, style = Kaiju.type.caption, color = colors.gold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            KaijuIconButton(KaijuIcons.Edit, "Edit clip ${c.clip.title}", onEdit, tint = colors.inkMuted)
            KaijuIconButton(KaijuIcons.Delete, "Delete clip ${c.clip.title}", onDelete, tint = colors.inkMuted)
        }
    }
}

@Composable
private fun LabelsSummary(video: MediaVideo, onEdit: () -> Unit) {
    val colors = Kaiju.colors
    val item = video.item
    Column(Modifier.fillMaxWidth()) {
        LabelLine("Type", item.kind.label)
        LabelLine("Position", item.position.ifBlank { "Not set" })
        Text("Techniques", style = Kaiju.type.caption, color = colors.inkMuted, modifier = Modifier.padding(top = KaijuSpace.xs))
        if (video.techniques.isEmpty()) {
            Text("None on the whole video", style = Kaiju.type.body, color = colors.ink)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs), verticalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                video.techniques.forEach { TechniqueTag(it.name) }
            }
        }
        LabelLine(
            "Who's in it",
            when {
                item.showsMinors -> "Students, including minors"
                item.showsStudents -> "Students"
                else -> "Only me / no students"
            },
        )
        if (item.notes.isNotBlank()) LabelLine("Notes", item.notes)
        LabelLine(
            "File",
            listOfNotNull(
                item.recordedAt?.let { "Recorded ${DATE.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))}" },
                formatBytes(item.sizeBytes),
                if (item.width > 0) "${item.width}×${item.height}" else null,
            ).joinToString(" · "),
        )
        Spacer(Modifier.height(KaijuSpace.s))
        KaijuButton("Edit labels", onEdit, variant = ButtonVariant.Secondary, icon = KaijuIcons.Edit)
    }
}

private val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

@Composable
private fun LabelLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(top = KaijuSpace.xs).semantics(mergeDescendants = true) {}) {
        Text(label, style = Kaiju.type.caption, color = Kaiju.colors.inkMuted)
        Text(value, style = Kaiju.type.body, color = Kaiju.colors.ink)
    }
}

@Composable
private fun TechniquePicker(all: List<TechniqueRef>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    Text("Techniques", style = Kaiju.type.label, color = Kaiju.colors.ink)
    Spacer(Modifier.height(KaijuSpace.xs))
    if (all.isEmpty()) {
        Text("Add techniques in Library › Techniques to label videos with them.", style = Kaiju.type.bodyS, color = Kaiju.colors.inkMuted)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
        all.forEach { t ->
            val on = t.id in selected
            KaijuChip(t.name, on, { onChange(if (on) selected - t.id else selected + t.id) })
        }
    }
}

@Composable
private fun DetailsSheet(
    video: MediaVideo,
    techniques: List<TechniqueRef>,
    positions: List<String>,
    onDismiss: () -> Unit,
    onSave: (MediaDetailsDraft) -> Unit,
) {
    val item = video.item
    var draft by remember(item.id) {
        mutableStateOf(
            MediaDetailsDraft(
                item.id, item.title, item.kind, item.position, item.notes,
                item.showsStudents, item.showsMinors, video.techniques.map { it.id }.toSet(),
            ),
        )
    }
    KaijuBottomSheet(onDismiss = onDismiss, title = "Labels", eyebrow = "Video", modifier = Modifier.imePadding()) {
        KaijuTextField(
            draft.title, { draft = draft.copy(title = it) }, "Title",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.m))
        Text("Type", style = Kaiju.type.label, color = Kaiju.colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            MediaKind.entries.forEach { k -> KaijuChip(k.label, draft.kind == k, { draft = draft.copy(kind = k) }) }
        }
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuTextField(
            draft.position, { draft = draft.copy(position = it) }, "Position",
            placeholder = "Closed guard",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            positions.forEach { p ->
                val on = draft.position.equals(p, ignoreCase = true)
                KaijuChip(p, on, { draft = draft.copy(position = if (on) "" else p) })
            }
        }
        Spacer(Modifier.height(KaijuSpace.m))
        TechniquePicker(techniques, draft.techniqueIds) { draft = draft.copy(techniqueIds = it) }
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuSwitchRow(
            "Shows students", draft.showsStudents || draft.showsMinors,
            { draft = draft.copy(showsStudents = it, showsMinors = if (it) draft.showsMinors else false) },
            subtitle = "Sharing will ask first",
            icon = KaijuIcons.Students,
        )
        KaijuSwitchRow(
            "Shows minors", draft.showsMinors,
            { draft = draft.copy(showsMinors = it, showsStudents = draft.showsStudents || it) },
            subtitle = "Anyone under 18. Sharing will warn you.",
            icon = KaijuIcons.Shield,
        )
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuTextField(
            draft.notes, { draft = draft.copy(notes = it) }, "Notes",
            placeholder = "What to watch for",
            singleLine = false,
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.l))
        KaijuButton("Save labels", { onSave(draft) }, Modifier.fillMaxWidth(), icon = KaijuIcons.Check)
    }
}

@Composable
private fun ClipSheet(
    initial: ClipDraft,
    techniques: List<TechniqueRef>,
    onDismiss: () -> Unit,
    onSave: (ClipDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val start = minOf(draft.startMs, draft.endMs)
    val end = maxOf(draft.startMs, draft.endMs)
    KaijuBottomSheet(
        onDismiss = onDismiss,
        title = if (initial.id == null) "Save clip" else "Edit clip",
        eyebrow = "${clockLabel(start)}–${clockLabel(end)}",
        modifier = Modifier.imePadding(),
    ) {
        KaijuTextField(
            draft.title, { draft = draft.copy(title = it) }, "Name",
            placeholder = "Knee cut entry",
            supportingText = "Leave empty to name it by its start time.",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.m))
        TechniquePicker(techniques, draft.techniqueIds) { draft = draft.copy(techniqueIds = it) }
        Spacer(Modifier.height(KaijuSpace.m))
        KaijuTextField(
            draft.notes, { draft = draft.copy(notes = it) }, "Notes",
            placeholder = "Key detail to point out",
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(KaijuSpace.l))
        KaijuButton(if (initial.id == null) "Save clip" else "Save changes", { onSave(draft) }, Modifier.fillMaxWidth(), icon = KaijuIcons.Check)
    }
}
