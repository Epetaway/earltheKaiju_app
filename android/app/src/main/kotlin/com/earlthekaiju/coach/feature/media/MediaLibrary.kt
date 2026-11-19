package com.earlthekaiju.coach.feature.media

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.data.media.MediaRepository
import com.earlthekaiju.coach.data.media.MediaVideo
import com.earlthekaiju.coach.data.media.TransferState
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuCard
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.domain.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** The Media half of the Library tab (Sprint 03). */
data class MediaTabState(
    val videos: List<MediaVideo> = emptyList(),
    val query: String = "",
    val kind: MediaKind? = null,
    val transfer: TransferState = TransferState(),
    val usedBytes: Long = 0L,
    /** Folder holding the thumbnails; null in previews and screenshots. */
    val thumbDir: File? = null,
) {
    val filtered: List<MediaVideo>
        get() = videos.filter { (kind == null || it.item.kind == kind) && it.matches(query) }

    val kindsPresent: List<MediaKind>
        get() = MediaKind.entries.filter { k -> videos.any { it.item.kind == k } }
}

data class MediaTabActions(
    val onImport: () -> Unit = {},
    val onExportAll: () -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onKind: (MediaKind?) -> Unit = {},
    val onOpenVideo: (String) -> Unit = {},
    val onDismissTransfer: () -> Unit = {},
)

fun LazyListScope.mediaTab(state: MediaTabState, actions: MediaTabActions) {
    item(key = "media-transfer") {
        TransferCard(state.transfer, actions.onDismissTransfer, Modifier.readableWidth())
    }
    if (state.videos.isEmpty()) {
        item(key = "media-empty") {
            EmptyState(
                eyebrow = "Media library",
                title = "Bring in your technique videos",
                body = "Pick videos from this phone. Copies go into a private folder only Kaiju Coach can see; " +
                    "they don't show in Photos or other apps. Label them by technique and position, and mark clips " +
                    "inside longer videos. Uninstalling the app deletes this folder, so save copies to a folder " +
                    "now and then (Library › Media › Save all to a folder).",
                primary = StateAction("Import videos", actions.onImport),
                modifier = Modifier.readableWidth(),
            )
        }
        return
    }
    item(key = "media-search") {
        Column(Modifier.readableWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                KaijuTextField(
                    state.query, actions.onQuery, "Search videos",
                    Modifier.weight(1f),
                    placeholder = "Title, technique, position, clip",
                    leadingIcon = KaijuIcons.Search,
                )
                Spacer(Modifier.width(KaijuSpace.s))
                KaijuIconButton(KaijuIcons.Import, "Import videos", actions.onImport, filled = true, size = 52.dp)
            }
            Spacer(Modifier.height(KaijuSpace.xs))
            Text(
                "${videoCount(state.videos.size)} · ${formatBytes(state.usedBytes)} on this phone",
                style = Kaiju.type.caption,
                color = Kaiju.colors.inkMuted,
            )
            if (state.kindsPresent.size > 1) {
                Spacer(Modifier.height(KaijuSpace.xs))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
                    KaijuChip("All", state.kind == null, { actions.onKind(null) })
                    state.kindsPresent.forEach { k -> KaijuChip(k.label, state.kind == k, { actions.onKind(if (state.kind == k) null else k) }) }
                }
            }
        }
    }
    val shown = state.filtered
    item(key = "media-header") {
        SectionHeader(
            if (shown.size == state.videos.size) "Videos · ${shown.size}" else "Matching · ${shown.size} of ${state.videos.size}",
            index = 1,
            modifier = Modifier.readableWidth().padding(top = KaijuSpace.m),
        )
    }
    if (shown.isEmpty()) {
        item(key = "media-none") {
            Text(
                "No videos match. Try another word, or clear the filter.",
                style = Kaiju.type.body,
                color = Kaiju.colors.inkMuted,
                modifier = Modifier.readableWidth().padding(vertical = KaijuSpace.s),
            )
        }
    }
    items(shown, key = { "media-${it.id}" }) { v ->
        MediaVideoCard(
            v,
            state.thumbDir?.let { File(it, "${v.id}.jpg") },
            { actions.onOpenVideo(v.id) },
            Modifier.readableWidth().padding(top = KaijuSpace.xs),
        )
    }
    item(key = "media-export") {
        Column(Modifier.readableWidth().padding(top = KaijuSpace.l, bottom = KaijuSpace.xl)) {
            KaijuButton(
                "Save all to a folder",
                actions.onExportAll,
                Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                icon = KaijuIcons.Export,
                enabled = !state.transfer.running,
            )
            Spacer(Modifier.height(KaijuSpace.xs))
            Text(
                "Copies every video plus a labels file to a folder you choose. The videos live only in this " +
                    "app's private folder, which is deleted if the app is uninstalled.",
                style = Kaiju.type.caption,
                color = Kaiju.colors.inkMuted,
            )
        }
    }
}

@Composable
private fun TransferCard(t: TransferState, onDismiss: () -> Unit, modifier: Modifier) {
    if (t.kind == null) return
    val colors = Kaiju.colors
    KaijuCard(modifier.padding(bottom = KaijuSpace.m), accentEdge = if (t.failed.isEmpty()) colors.action else colors.signal) {
        val verb = if (t.kind == TransferState.Kind.IMPORT) "Importing" else "Saving"
        if (t.running) {
            Text("$verb ${t.done + 1} of ${t.total}", style = Kaiju.type.label, color = colors.ink)
            Text(t.current, style = Kaiju.type.caption, color = colors.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(KaijuSpace.xs))
            Box(Modifier.fillMaxWidth().height(4.dp).background(colors.surfaceInset)) {
                Box(Modifier.fillMaxWidth(if (t.total == 0) 0f else t.done.toFloat() / t.total).height(4.dp).background(colors.action))
            }
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text("You can leave this screen. Keep the app open until it finishes.", style = Kaiju.type.caption, color = colors.inkMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.result.orEmpty(), style = Kaiju.type.label, color = colors.ink)
                    t.failed.forEach { Text(it, style = Kaiju.type.caption, color = colors.inkMuted) }
                }
                KaijuIconButton(KaijuIcons.Close, "Dismiss", onDismiss, tint = colors.inkMuted)
            }
        }
    }
}

@Composable
fun MediaVideoCard(video: MediaVideo, thumbnail: File?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val item = video.item
    KaijuCard(modifier, onClick = onClick, onClickLabel = "Open video", contentPadding = KaijuSpace.s) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.width(120.dp)) {
                MediaThumbnail(thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                if (item.durationMs > 0) {
                    Text(
                        MediaRepository.clockLabel(item.durationMs),
                        style = Kaiju.type.caption,
                        color = colors.ink,
                        modifier = Modifier.align(Alignment.BottomEnd).background(colors.background).padding(horizontal = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(KaijuSpace.s))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = Kaiju.type.label, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(item.kind.label, item.position).filter { it.isNotBlank() }.joinToString(" · "),
                    style = Kaiju.type.caption,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val techniques = video.allTechniques
                if (techniques.isNotEmpty()) {
                    Text(
                        techniques.joinToString(" · ") { it.name },
                        style = Kaiju.type.caption,
                        color = colors.gold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (video.clips.isNotEmpty()) {
                        Icon(KaijuIcons.Clip, null, tint = colors.inkMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(if (video.clips.size == 1) "1 clip" else "${video.clips.size} clips", style = Kaiju.type.caption, color = colors.inkMuted)
                        Spacer(Modifier.width(KaijuSpace.xs))
                    }
                    if (item.showsMinors || item.showsStudents) {
                        Icon(KaijuIcons.Shield, null, tint = colors.gold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(if (item.showsMinors) "Shows minors" else "Shows students", style = Kaiju.type.caption, color = colors.gold)
                    }
                }
            }
        }
    }
}

/** Loads a small JPEG thumbnail off the main thread; a plain framed icon until (or unless) it exists. */
@Composable
fun MediaThumbnail(file: File?, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    val bitmap by produceState<ImageBitmap?>(null, file?.path) {
        value = file?.let {
            withContext(Dispatchers.IO) { if (it.exists()) BitmapFactory.decodeFile(it.path)?.asImageBitmap() else null }
        }
    }
    Box(modifier.background(colors.surfaceInset).border(1.dp, colors.line, KaijuShapes.Small), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(KaijuIcons.Video, null, tint = colors.inkMuted, modifier = Modifier.size(24.dp))
        }
    }
}

fun videoCount(n: Int): String = if (n == 1) "1 video" else "$n videos"

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(Locale.ROOT, "%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(Locale.ROOT, "%.0f MB", bytes / 1_000_000.0)
    bytes > 0L -> String.format(Locale.ROOT, "%.0f KB", maxOf(1.0, bytes / 1_000.0))
    else -> "0 MB"
}
