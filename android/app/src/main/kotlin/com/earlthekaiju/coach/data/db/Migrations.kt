package com.earlthekaiju.coach.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change ships with a hand-written, tested migration (docs/05, "Migrations").
 * There is deliberately no destructive fallback: losing a roster is not acceptable.
 */
object KaijuMigrations {

    /**
     * 1 → 2 (Sprint 03): adds the private media library. New tables only; nothing existing changes.
     * The SQL matches what Room generates for the v2 entities exactly (Room checks it on open,
     * and MigrationTest proves it).
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `media_item` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`fileName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, " +
                    "`durationMs` INTEGER NOT NULL, `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, " +
                    "`kind` TEXT NOT NULL, `position` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                    "`showsStudents` INTEGER NOT NULL, `showsMinors` INTEGER NOT NULL, `recordedAt` INTEGER, " +
                    "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `media_clip` (`id` TEXT NOT NULL, `mediaId` TEXT NOT NULL, " +
                    "`title` TEXT NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER NOT NULL, " +
                    "`notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                    "`deletedAt` INTEGER, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`mediaId`) REFERENCES `media_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_clip_mediaId` ON `media_clip` (`mediaId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `media_technique` (`mediaId` TEXT NOT NULL, `techniqueId` TEXT NOT NULL, " +
                    "PRIMARY KEY(`mediaId`, `techniqueId`), " +
                    "FOREIGN KEY(`mediaId`) REFERENCES `media_item`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`techniqueId`) REFERENCES `technique`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_technique_techniqueId` ON `media_technique` (`techniqueId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `clip_technique` (`clipId` TEXT NOT NULL, `techniqueId` TEXT NOT NULL, " +
                    "PRIMARY KEY(`clipId`, `techniqueId`), " +
                    "FOREIGN KEY(`clipId`) REFERENCES `media_clip`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`techniqueId`) REFERENCES `technique`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_clip_technique_techniqueId` ON `clip_technique` (`techniqueId`)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
