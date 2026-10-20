package com.earlthekaiju.coach.data.backup

import com.earlthekaiju.coach.data.db.AttendanceEntity
import com.earlthekaiju.coach.data.db.ClassSessionEntity
import com.earlthekaiju.coach.data.db.EnrollmentEntity
import com.earlthekaiju.coach.data.db.KaijuDatabase
import com.earlthekaiju.coach.data.db.LessonPlanEntity
import com.earlthekaiju.coach.data.db.LessonSegmentEntity
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.db.SegmentTechniqueCrossRef
import com.earlthekaiju.coach.data.db.SessionNoteEntity
import com.earlthekaiju.coach.data.db.SessionSegmentEntity
import com.earlthekaiju.coach.data.db.StudentEntity
import com.earlthekaiju.coach.data.db.TechniqueEntity
import com.earlthekaiju.coach.data.db.TimerPresetEntity
import com.earlthekaiju.coach.data.prefs.SettingsRepository
import com.earlthekaiju.coach.domain.time.AppClock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalTime

object LocalDateIso : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

object LocalTimeIso : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalTime = LocalTime.parse(decoder.decodeString())
}

/**
 * The backup / interchange format, version 1. Mirrors the Room tables one to one, tombstones
 * included. The same shape is described for the desktop companion in
 * contracts/kaiju-coach-backup.v1.schema.json.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val formatVersion: Int = 1,
    val databaseVersion: Int,
    val exportedAt: Long,
    val deviceId: String,
    val app: String = "Kaiju Coach",
    val programs: List<ProgramEntity>,
    val lessonPlans: List<LessonPlanEntity>,
    val lessonSegments: List<LessonSegmentEntity>,
    val techniques: List<TechniqueEntity>,
    val segmentTechniques: List<SegmentTechniqueCrossRef>,
    val classSessions: List<ClassSessionEntity>,
    val sessionSegments: List<SessionSegmentEntity>,
    val sessionNotes: List<SessionNoteEntity>,
    val students: List<StudentEntity>,
    val enrollments: List<EnrollmentEntity>,
    val attendance: List<AttendanceEntity>,
    val timerPresets: List<TimerPresetEntity>,
) {
    companion object {
        const val FORMAT = "kaiju-coach-backup"
    }
}

data class ExportResult(val bytes: Int, val records: Int)

/**
 * Writes a full JSON backup to a file the instructor picks (Storage Access Framework), so the
 * data can leave the phone without any cloud service. The file contains student names: the
 * Settings screen says so before exporting.
 */
class BackupExporter(
    private val db: KaijuDatabase,
    private val settings: SettingsRepository,
    private val clock: AppClock,
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun build(): BackupFile {
        val d = db.backupDao()
        return BackupFile(
            databaseVersion = 1,
            exportedAt = clock.now(),
            deviceId = settings.deviceId(),
            programs = d.programs(),
            lessonPlans = d.lessonPlans(),
            lessonSegments = d.lessonSegments(),
            techniques = d.techniques(),
            segmentTechniques = d.segmentTechniques(),
            classSessions = d.sessions(),
            sessionSegments = d.sessionSegments(),
            sessionNotes = d.sessionNotes(),
            students = d.students(),
            enrollments = d.enrollments(),
            attendance = d.attendance(),
            timerPresets = d.timerPresets(),
        )
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    suspend fun writeTo(out: OutputStream): ExportResult {
        val file = build()
        val bytes = encode(file).toByteArray(Charsets.UTF_8)
        out.write(bytes)
        out.flush()
        settings.setLastBackupAt(file.exportedAt)
        val records = with(file) {
            programs.size + lessonPlans.size + lessonSegments.size + techniques.size + segmentTechniques.size +
                classSessions.size + sessionSegments.size + sessionNotes.size + students.size + enrollments.size +
                attendance.size + timerPresets.size
        }
        return ExportResult(bytes.size, records)
    }
}
