package com.osudroid.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.LegacyModConverter
import com.rian.osu.mods.ModReplayV6
import com.rian.osu.utils.ModUtils
import java.io.File

/**
 * Base class for migrations. Backups the database file before performing the migration.
 *
 * @param startVersion The version of the database before the migration.
 * @param endVersion The version of the database after the migration.
 */
abstract class BackedUpMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    final override fun migrate(db: SupportSQLiteDatabase) {
        val dbFile = File(DatabaseManager.databasePath)

        if (dbFile.exists()) {
            val backupFile = File(
                dbFile.parent,
                "${dbFile.nameWithoutExtension}_version${startVersion}_${System.currentTimeMillis()}.db"
            )

            dbFile.copyTo(backupFile, true)
        }

        performMigration(db)
    }

    /**
     * Performs the actual migration logic.
     *
     * @param db The database to migrate.
     */
    protected abstract fun performMigration(db: SupportSQLiteDatabase)
}

val MIGRATION_1_2 = object : BackedUpMigration(1, 2) {
    override fun performMigration(db: SupportSQLiteDatabase) {
        db.query("SELECT id, mods, beatmapMD5 from ScoreInfo").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val oldMods = cursor.getString(1)
                val beatmapMD5 = cursor.getString(2)

                val difficulty = db.query(
                    "SELECT circleSize, approachRate, overallDifficulty, hpDrainRate FROM BeatmapInfo WHERE md5 = ?",
                    arrayOf(beatmapMD5)
                ).use { cursor ->
                    if (cursor.count == 0) {
                        return@use null
                    }

                    cursor.moveToFirst()
                    BeatmapDifficulty(cursor.getFloat(0), cursor.getFloat(1), cursor.getFloat(2), cursor.getFloat(3))
                }

                try {
                    // Check if the mods are already in the new format. In that case, we don't need to migrate them.
                    // Realistically, this should never happen since migrations are done in a transaction, but there are
                    // crash reports where the CREATE TABLE statement below for ModPreset was executed more than once
                    // (hence the addition of the IF NOT EXISTS clause to that statement).
                    ModUtils.deserializeMods(oldMods)
                } catch (_: Exception) {
                    // If the mods are not deserializable, we assume they are legacy mods, so we migrate them.
                    val newMods = LegacyModConverter.convert(oldMods, difficulty)
                    newMods.put(ModReplayV6())

                    db.execSQL(
                        "UPDATE ScoreInfo SET mods = ? WHERE id = ?",
                        arrayOf<Any>(newMods.serializeMods().toString(), id)
                    )
                }
            }
        }

        db.execSQL("ALTER TABLE BeatmapInfo ADD COLUMN epilepsyWarning INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS ModPreset (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "serializedMods TEXT NOT NULL" +
            ")"
        )
    }
}