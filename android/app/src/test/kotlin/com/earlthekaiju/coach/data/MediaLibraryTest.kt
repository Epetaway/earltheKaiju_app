package com.earlthekaiju.coach.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.db.KaijuMigrations
import com.earlthekaiju.coach.data.db.MediaItemEntity
import com.earlthekaiju.coach.data.media.ClipDraft
import com.earlthekaiju.coach.data.media.MediaDetailsDraft
import com.earlthekaiju.coach.data.media.MediaFiles
import com.earlthekaiju.coach.data.media.MediaRepository
import com.earlthekaiju.coach.data.repo.TechniqueDraft
import com.earlthekaiju.coach.data.repo.TechniqueRepository
import com.earlthekaiju.coach.domain.model.MediaKind
import com.earlthekaiju.coach.domain.time.AppClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneId

private class MediaTestClock(var nowMillis: Long = 1_792_521_000_000L) : AppClock {
    override fun now(): Long = nowMillis
    override fun elapsedRealtime(): Long = nowMillis
    override fun zone(): ZoneId = ZoneId.of("America/New_York")
}

/**
 * The media library's data (Sprint 03): the 1 → 2 migration on a real version-1 database, and
 * how videos, clips and technique labels come together.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
class MediaLibraryTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val clock = MediaTestClock()
    private lateinit var dir: File

    @Before fun setUp() {
        dir = File(context.cacheDir, "media-test-${System.nanoTime()}").apply { mkdirs() }
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    /**
     * Builds the exact version-1 database from the committed schema (schemas/…/1.json), with a
     * technique in it, then opens it with today's Room and the migration. Room checks every new
     * table, column, key and index against the entities on open, so a wrong migration fails here.
     */
    @Test
    fun migratesAVersionOneDatabaseAndKeepsItsData() = runTest {
        val file = File(dir, "v1.db")
        val schema = JSONObject(File("schemas/com.earlthekaiju.coach.data.db.KaijuDatabase/1.json").readText()).getJSONObject("database")
        assertEquals(1, schema.getInt("version"))
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                v1.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices")
                if (indices != null) {
                    for (j in 0 until indices.length()) v1.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v1.execSQL(setup.getString(i))
            v1.execSQL("INSERT INTO technique (id, name, category, notes, createdAt, updatedAt, deletedAt) VALUES ('t1', 'Knee cut', 'Passing', '', 1, 1, NULL)")
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, KaijuDatabase::class.java, file.absolutePath)
            .addMigrations(*KaijuMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(2, db.openHelper.writableDatabase.version)
            assertEquals("Knee cut", db.techniqueDao().get("t1")?.name)

            // The new tables work end to end on the migrated file.
            val media = MediaRepository(db, clock, MediaFiles(File(dir, "media")))
            media.add(video("v1"))
            media.saveDetails(details("v1", setOf("t1")))
            media.saveClip(ClipDraft(mediaId = "v1", title = "Entry", startMs = 2_000, endMs = 9_000, techniqueIds = setOf("t1")))
            val library = media.observeLibrary().first()
            assertEquals(listOf("Knee cut"), library.single().techniques.map { it.name })
            assertEquals("Entry", library.single().clips.single().clip.title)
        } finally {
            db.close()
        }
    }

    @Test
    fun videosClipsAndTechniqueLabelsComeTogether() = runTest {
        val db = KaijuDatabase.inMemory(context)
        try {
            val techniques = TechniqueRepository(db, clock)
            val kneeCut = techniques.save(TechniqueDraft(name = "Knee cut", category = "Passing"))
            val hipEscape = techniques.save(TechniqueDraft(name = "Hip escape", category = "Escapes"))
            val files = MediaFiles(File(dir, "media"))
            val media = MediaRepository(db, clock, files)

            files.root.mkdirs()
            File(files.root, "a.mp4").writeText("video")
            media.add(video("a", fileName = "a.mp4", durationMs = 60_000))
            media.add(video("b", durationMs = 30_000))

            media.saveDetails(details("a", setOf(kneeCut)).copy(position = " Half guard ", showsMinors = true))
            // Times given backwards and past the end are put in order and kept inside the video.
            val clipId = media.saveClip(ClipDraft(mediaId = "b", startMs = 45_000, endMs = 20_000, techniqueIds = setOf(hipEscape)))

            val library = media.observeLibrary().first()
            val a = library.first { it.id == "a" }
            assertEquals("Half guard", a.item.position)
            assertTrue("minors implies students", a.item.showsStudents && a.item.showsMinors)
            assertEquals(listOf("Knee cut"), a.techniques.map { it.name })

            val clip = library.first { it.id == "b" }.clips.single()
            assertEquals(clipId, clip.id)
            assertEquals(20_000L, clip.clip.startMs)
            assertEquals(30_000L, clip.clip.endMs)
            assertEquals("Clip at 0:20", clip.clip.title)

            // A technique's page finds videos through the whole video or through a clip.
            assertEquals(listOf("a"), media.observeForTechnique(kneeCut).first().map { it.id })
            assertEquals(listOf("b"), media.observeForTechnique(hipEscape).first().map { it.id })

            // Search covers technique names on clips, positions and titles.
            assertTrue(library.first { it.id == "b" }.matches("hip esc"))
            assertTrue(a.matches("half"))
            assertFalse(a.matches("mount"))

            // A deleted technique stops labelling anything.
            techniques.delete(hipEscape)
            assertTrue(media.observeForTechnique(hipEscape).first().isEmpty())

            // Deleting a clip keeps the video; deleting a video removes its file too.
            media.deleteClip(clipId)
            assertTrue(media.observeLibrary().first().first { it.id == "b" }.clips.isEmpty())
            media.delete("a")
            assertEquals(listOf("b"), media.observeLibrary().first().map { it.id })
            assertFalse(File(files.root, "a.mp4").exists())
        } finally {
            db.close()
        }
    }

    @Test
    fun clockLabels() {
        assertEquals("0:00", MediaRepository.clockLabel(0))
        assertEquals("1:23", MediaRepository.clockLabel(83_400))
        assertEquals("1:02:03", MediaRepository.clockLabel(3_723_000))
    }

    private fun video(id: String, fileName: String = "$id.mp4", durationMs: Long = 60_000) = MediaItemEntity(
        id = id, title = "Video $id", fileName = fileName, mimeType = "video/mp4", sizeBytes = 5,
        durationMs = durationMs, width = 1920, height = 1080, createdAt = clock.now(), updatedAt = clock.now(),
    )

    private fun details(id: String, techniqueIds: Set<String>) = MediaDetailsDraft(
        id = id, title = "Video $id", kind = MediaKind.INSTRUCTION, position = "", notes = "",
        showsStudents = false, showsMinors = false, techniqueIds = techniqueIds,
    )
}
