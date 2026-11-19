package com.earlthekaiju.coach.data.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.earlthekaiju.coach.data.db.MediaItemEntity
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.time.newId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Progress of an import into, or an export out of, the private media folder. */
data class TransferState(
    val kind: Kind? = null,
    val running: Boolean = false,
    val total: Int = 0,
    val done: Int = 0,
    val current: String = "",
    val failed: List<String> = emptyList(),
    /** Plain-language summary once a run finishes, until dismissed. */
    val result: String? = null,
) {
    enum class Kind { IMPORT, EXPORT }
}

/**
 * Copies picked videos into the app's private folder, and copies them back out to a folder the
 * instructor chooses. One transfer at a time; runs on the app scope so leaving the screen
 * doesn't stop it.
 */
class MediaTransfers(
    context: Context,
    private val repo: MediaRepository,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val files = repo.files
    private val mutex = Mutex()
    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch { importAll(uris) }
    }

    fun exportAll(treeUri: Uri) {
        scope.launch { exportAllTo(treeUri) }
    }

    fun dismissResult() = _state.update { if (it.running) it else TransferState() }

    suspend fun importAll(uris: List<Uri>) = mutex.withLock {
        _state.value = TransferState(TransferState.Kind.IMPORT, running = true, total = uris.size)
        val failed = mutableListOf<String>()
        var imported = 0
        uris.forEachIndexed { i, uri ->
            val name = withContext(Dispatchers.IO) { displayName(uri) } ?: "Video ${i + 1}"
            _state.update { it.copy(current = name) }
            val ok = withContext(Dispatchers.IO) { runCatching { importOne(uri, name) } }
            if (ok.isSuccess) imported++ else failed += reason(name, ok.exceptionOrNull())
            _state.update { it.copy(done = i + 1) }
        }
        _state.value = TransferState(
            TransferState.Kind.IMPORT,
            running = false,
            total = uris.size,
            done = uris.size,
            failed = failed,
            result = when {
                failed.isEmpty() -> if (imported == 1) "1 video added" else "$imported videos added"
                imported == 0 -> "Nothing was added"
                else -> "$imported added, ${failed.size} couldn't be"
            },
        )
    }

    private fun reason(name: String, e: Throwable?): String = when (e) {
        is NotEnoughSpace -> "$name: not enough free space on the phone"
        else -> "$name: couldn't be read"
    }

    private class NotEnoughSpace : IOException()

    private suspend fun importOne(uri: Uri, displayName: String) {
        val id = newId()
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("video/") } ?: "video/mp4"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp4"
        val declaredSize = size(uri)
        if (declaredSize > 0 && files.freeBytes() < declaredSize + SPACE_MARGIN_BYTES) throw NotEnoughSpace()

        files.root.mkdirs()
        val target = File(files.root, "$id.$ext")
        val partial = File(files.root, "$id.$ext.part")
        try {
            val input = resolver.openInputStream(uri) ?: throw IOException("No stream")
            input.use { inp -> partial.outputStream().use { out -> inp.copyTo(out, BUFFER) } }
            if (!partial.renameTo(target)) throw IOException("Rename failed")
            val meta = readMetadata(target)
            writeThumbnail(target, id, meta.durationMs)
            val now = clock.now()
            repo.add(
                MediaItemEntity(
                    id = id,
                    title = displayName.substringBeforeLast('.').ifBlank { "Video" },
                    fileName = target.name,
                    mimeType = mime,
                    sizeBytes = target.length(),
                    durationMs = meta.durationMs,
                    width = meta.width,
                    height = meta.height,
                    recordedAt = meta.recordedAt,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        } catch (e: Exception) {
            partial.delete()
            target.delete()
            files.thumbnail(id).delete()
            throw e
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun size(uri: Uri): Long = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)

    private data class Meta(val durationMs: Long, val width: Int, val height: Int, val recordedAt: Long?)

    private fun readMetadata(file: File): Meta {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val portrait = rotation == 90 || rotation == 270
            Meta(
                durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                width = if (portrait) h else w,
                height = if (portrait) w else h,
                recordedAt = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let(::parseDate),
            )
        } catch (_: Exception) {
            Meta(0L, 0, 0, null)
        } finally {
            runCatching { r.release() }
        }
    }

    /** Phones write e.g. "20261118T183012.000Z". Unknown formats are ignored. */
    private fun parseDate(raw: String): Long? = runCatching {
        SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.ROOT)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(raw)?.time?.takeIf { it > 0 }
    }.getOrNull()

    private fun writeThumbnail(file: File, id: String, durationMs: Long) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file.absolutePath)
            val atUs = (minOf(1_000L, durationMs / 2)).coerceAtLeast(0L) * 1_000L
            val frame = r.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return
            val scale = THUMB_WIDTH.toFloat() / frame.width
            val thumb = if (scale < 1f) Bitmap.createScaledBitmap(frame, THUMB_WIDTH, (frame.height * scale).toInt().coerceAtLeast(1), true) else frame
            files.thumbDir.mkdirs()
            files.thumbnail(id).outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        } catch (_: Exception) {
            // A missing thumbnail only means the list shows a plain frame.
        } finally {
            runCatching { r.release() }
        }
    }

    /**
     * Copies every video into the chosen folder, plus kaiju-media-labels.json with titles, labels
     * and clips, so nothing is lost if the app is uninstalled or the phone is replaced.
     */
    suspend fun exportAllTo(treeUri: Uri) = mutex.withLock {
        val videos = repo.observeLibrary().first()
        _state.value = TransferState(TransferState.Kind.EXPORT, running = true, total = videos.size)
        val failed = mutableListOf<String>()
        val folder = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val usedNames = mutableSetOf<String>()
        val index = JSONArray()
        videos.forEachIndexed { i, v ->
            _state.update { it.copy(current = v.item.title) }
            val name = uniqueName(safeName(v.item.title), v.item.fileName.substringAfterLast('.', "mp4"), usedNames)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val doc = DocumentsContract.createDocument(resolver, folder, v.item.mimeType, name) ?: throw IOException("Create failed")
                    val out = resolver.openOutputStream(doc) ?: throw IOException("No stream")
                    out.use { o -> files.video(v.item).inputStream().use { it.copyTo(o, BUFFER) } }
                }
            }
            if (ok.isSuccess) index.put(labels(v, name)) else failed += v.item.title
            _state.update { it.copy(done = i + 1) }
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val doc = DocumentsContract.createDocument(resolver, folder, "application/json", "kaiju-media-labels.json")
                doc?.let { resolver.openOutputStream(it)?.use { o -> o.write(JSONObject().put("videos", index).toString(2).toByteArray()) } }
            }
        }
        _state.value = TransferState(
            TransferState.Kind.EXPORT,
            running = false,
            total = videos.size,
            done = videos.size,
            failed = failed,
            result = if (failed.isEmpty()) "${videos.size} videos saved to the folder, with their labels" else "${videos.size - failed.size} saved, ${failed.size} couldn't be",
        )
    }

    private fun labels(v: MediaVideo, exportedName: String): JSONObject = JSONObject()
        .put("file", exportedName)
        .put("title", v.item.title)
        .put("kind", v.item.kind.name)
        .put("position", v.item.position)
        .put("notes", v.item.notes)
        .put("showsStudents", v.item.showsStudents)
        .put("showsMinors", v.item.showsMinors)
        .put("techniques", JSONArray(v.techniques.map { it.name }))
        .put(
            "clips",
            JSONArray(
                v.clips.map { c ->
                    JSONObject()
                        .put("title", c.clip.title)
                        .put("startMs", c.clip.startMs)
                        .put("endMs", c.clip.endMs)
                        .put("notes", c.clip.notes)
                        .put("techniques", JSONArray(c.techniques.map { it.name }))
                },
            ),
        )

    private fun safeName(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim().take(80).ifBlank { "Video" }

    private fun uniqueName(base: String, ext: String, used: MutableSet<String>): String {
        var candidate = "$base.$ext"
        var n = 2
        while (!used.add(candidate.lowercase())) candidate = "$base ($n).$ext".also { n++ }
        return candidate
    }

    private companion object {
        const val BUFFER = 1 shl 16
        const val THUMB_WIDTH = 480
        /** Keep this much free after an import so the phone itself never runs out. */
        const val SPACE_MARGIN_BYTES = 200L * 1024 * 1024
    }
}
