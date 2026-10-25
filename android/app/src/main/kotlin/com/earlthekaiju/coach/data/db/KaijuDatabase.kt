package com.earlthekaiju.coach.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** ISO-8601 text keeps dates and times sortable and readable in the SQLite file and in exports. */
class Converters {
    @TypeConverter fun dateToText(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun timeToText(value: LocalTime?): String? = value?.let { String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute) }
    @TypeConverter fun textToTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}

/**
 * Version 1 of the on-device database. Schemas are exported to android/app/schemas so every
 * future version ships with a tested Migration (see docs/05-data-model.md, "Migrations").
 * There is deliberately no fallbackToDestructiveMigration: losing a roster is not acceptable.
 */
@Database(
    entities = [
        ProgramEntity::class,
        LessonPlanEntity::class,
        LessonSegmentEntity::class,
        TechniqueEntity::class,
        SegmentTechniqueCrossRef::class,
        ClassSessionEntity::class,
        SessionSegmentEntity::class,
        StudentEntity::class,
        EnrollmentEntity::class,
        AttendanceEntity::class,
        TimerPresetEntity::class,
        SessionNoteEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KaijuDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun lessonDao(): LessonDao
    abstract fun techniqueDao(): TechniqueDao
    abstract fun sessionDao(): SessionDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timerPresetDao(): TimerPresetDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val FILE_NAME = "kaiju-coach.db"

        fun build(context: Context): KaijuDatabase =
            Room.databaseBuilder(context.applicationContext, KaijuDatabase::class.java, FILE_NAME)
                .build()

        fun inMemory(context: Context): KaijuDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, KaijuDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
