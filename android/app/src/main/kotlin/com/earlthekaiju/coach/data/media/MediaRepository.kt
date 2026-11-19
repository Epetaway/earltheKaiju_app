package com.earlthekaiju.coach.data.media

import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.db.MediaClipEntity
import com.earlthekaiju.coach.data.db.MediaItemEntity
import com.earlthekaiju.coach.domain.model.MediaKind
import com.earlthekaiju.coach.domain.time.AppClock
import com.earlthekaiju.coach.domain.time.newId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File

data class TechniqueRef(val id: String, val name: String)

data class MediaClip(val clip: MediaClipEntity, val techniques: List<TechniqueRef>) {
    val id: String get() = clip.id
}

/** A video with its labels and clips, as the screens use it. */
data class MediaVideo(
    val item: MediaItemEntity,
    val techniques: List<TechniqueRef>,
    val clips: List<MediaClip>,
) {
    val id: String get() = item.id

    /** Every technique on the video or any of its clips: for search and a technique's page. */
    val allTechniques: List<TechniqueRef>
        get() = (techniques + clips.flatMap { it.techniques }).distinctBy { it.id }.sortedBy { it.name.lowercase() }

    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return item.title.contains(q, true) ||
            item.position.contains(q, true) ||
            item.notes.contains(q, true) ||
            item.kind.label.contains(q, true) ||
            allTechniques.any { it.name.contains(q, true) } ||
            clips.any { it.clip.title.contains(q, true) }
    }
}

data class MediaDetailsDraft(
    val id: String,
    val title: String,
    val kind: MediaKind,
    val position: String,
    val notes: String,
    val showsStudents: Boolean,
    val showsMinors: Boolean,
    val techniqueIds: Set<String>,
)

data class ClipDraft(
    val id: String? = null,
    val mediaId: String,
    val title: String = "",
    val startMs: Long,
    val endMs: Long,
    val notes: String = "",
    val techniqueIds: Set<String> = emptySet(),
)

/** Where the videos physically live: the app's private folder, invisible to other apps. */
class MediaFiles(val root: File) {
    val thumbDir: File get() = File(root, "thumbs")

    fun video(item: MediaItemEntity): File = File(root, item.fileName)
    fun thumbnail(mediaId: String): File = File(thumbDir, "$mediaId.jpg")

    fun delete(item: MediaItemEntity) {
        video(item).delete()
        thumbnail(item.id).delete()
    }

    fun usedBytes(): Long = if (root.exists()) root.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

    fun freeBytes(): Long {
        root.mkdirs()
        return root.usableSpace
    }
}

class MediaRepository(db: KaijuDatabase, private val clock: AppClock, val files: MediaFiles) {
    private val dao = db.mediaDao()

    fun observeLibrary(): Flow<List<MediaVideo>> =
        combine(dao.observeItems(), dao.observeClips(), dao.observeTechniqueLinks()) { items, clips, links ->
            val byVideo = links.filter { it.clipId == null }.groupBy { it.mediaId }
            val byClip = links.filter { it.clipId != null }.groupBy { it.clipId }
            val clipsByVideo = clips.groupBy { it.mediaId }
            items.map { item ->
                MediaVideo(
                    item = item,
                    techniques = byVideo[item.id].orEmpty().map { TechniqueRef(it.techniqueId, it.techniqueName) }.sortedBy { it.name.lowercase() },
                    clips = clipsByVideo[item.id].orEmpty().map { clip ->
                        MediaClip(clip, byClip[clip.id].orEmpty().map { TechniqueRef(it.techniqueId, it.techniqueName) }.sortedBy { it.name.lowercase() })
                    },
                )
            }
        }

    fun observeVideo(id: String): Flow<MediaVideo?> = observeLibrary().map { all -> all.firstOrNull { it.id == id } }

    /** Videos showing a technique on the whole video or on any clip. */
    fun observeForTechnique(techniqueId: String): Flow<List<MediaVideo>> =
        observeLibrary().map { all -> all.filter { v -> v.allTechniques.any { it.id == techniqueId } } }

    suspend fun add(item: MediaItemEntity) = dao.insert(item)

    suspend fun liveItems(): List<MediaItemEntity> = dao.liveItems()

    suspend fun saveDetails(draft: MediaDetailsDraft) {
        val existing = dao.get(draft.id)?.takeIf { it.deletedAt == null } ?: return
        dao.saveDetails(
            existing.copy(
                title = draft.title.trim().ifBlank { existing.title },
                kind = draft.kind,
                position = draft.position.trim(),
                notes = draft.notes.trim(),
                // A video that shows minors shows students too.
                showsStudents = draft.showsStudents || draft.showsMinors,
                showsMinors = draft.showsMinors,
                updatedAt = clock.now(),
            ),
            draft.techniqueIds.toList(),
        )
    }

    /** Saves a clip. Start and end are put in order and kept inside the video. Returns its id. */
    suspend fun saveClip(draft: ClipDraft): String {
        val video = dao.get(draft.mediaId) ?: error("No video ${draft.mediaId}")
        val now = clock.now()
        val existing = draft.id?.let { dao.getClip(it) }
        val upper = if (video.durationMs > 0) video.durationMs else Long.MAX_VALUE
        var start = minOf(draft.startMs, draft.endMs).coerceIn(0L, upper)
        var end = maxOf(draft.startMs, draft.endMs).coerceIn(0L, upper)
        if (end - start < MIN_CLIP_MS) {
            end = (start + MIN_CLIP_MS).coerceAtMost(upper)
            start = (end - MIN_CLIP_MS).coerceAtLeast(0L)
        }
        val id = existing?.id ?: draft.id ?: newId()
        dao.saveClip(
            MediaClipEntity(
                id = id,
                mediaId = draft.mediaId,
                title = draft.title.trim().ifBlank { "Clip at ${clockLabel(start)}" },
                startMs = start,
                endMs = end,
                notes = draft.notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
            draft.techniqueIds.toList(),
        )
        return id
    }

    suspend fun deleteClip(id: String) = dao.softDeleteClip(id, clock.now())

    /** Removes the video from the library and deletes its file, freeing the space right away. */
    suspend fun delete(id: String) {
        val item = dao.get(id) ?: return
        dao.softDelete(id, clock.now())
        files.delete(item)
    }

    companion object {
        const val MIN_CLIP_MS = 1_000L

        /** 83_000 → "1:23"; 3_723_000 → "1:02:03". */
        fun clockLabel(ms: Long): String {
            val total = (ms / 1000).coerceAtLeast(0)
            val h = total / 3600
            val m = (total % 3600) / 60
            val s = total % 60
            return if (h > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT, h, m, s) else "%d:%02d".format(java.util.Locale.ROOT, m, s)
        }
    }
}
