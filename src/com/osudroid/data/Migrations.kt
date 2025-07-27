package com.osudroid.data

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.LegacyModConverter
import com.rian.osu.mods.ModRateAdjust
import com.rian.osu.mods.ModReplayV6
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils
import java.io.File
import kotlin.system.exitProcess
import ru.nsu.ccfit.zuev.osu.ToastLogger

/**
 * Base class for migrations. Backups the database file before performing the migration.
 *
 * @param startVersion The version of the database before the migration.
 * @param endVersion The version of the database after the migration.
 */
abstract class BackedUpMigration(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    final override fun migrate(db: SupportSQLiteDatabase) {
        try {
            val dbFile = File(DatabaseManager.databasePath)

            if (dbFile.exists()) {
                val backupFile = File(
                    dbFile.parent,
                    "${dbFile.nameWithoutExtension}_version${startVersion}_${System.currentTimeMillis()}.db"
                )

                dbFile.copyTo(backupFile, true)
            }

            performMigration(db)
        } catch (e: Exception) {
            val message = "Failed to perform database migration from version $startVersion to $endVersion"

            ToastLogger.showText("$message, exiting", true)
            Log.e("Migration", message, e)

            exitProcess(1)
        }
    }

    /**
     * Performs the actual migration logic.
     *
     * @param db The database to migrate.
     */
    protected abstract fun performMigration(db: SupportSQLiteDatabase)
}

/**
 * Migration from version 1 to 2.
 *
 * Contains the following changes:
 * - Migrates legacy mods format in the `ScoreInfo` table to the new format.
 * - Adds the epilepsyWarning column to the `BeatmapInfo` table.
 * - Creates the `ModPreset` table.
 */
val MIGRATION_1_2 = object : BackedUpMigration(1, 2) {
    override fun performMigration(db: SupportSQLiteDatabase) {
        db.query("SELECT id, mods, beatmapMD5 from ScoreInfo").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val oldMods = cursor.getString(1)
                val beatmapMD5 = cursor.getString(2)

                @Suppress("DuplicatedCode")
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

/**
 * Migration from version 2 to 3.
 *
 * Contains the following changes:
 * - Adds slider tick and end hits statistics to `ScoreInfo`
 * - Detects if mods were not migrated properly in version 1 to 2 migration and migrates them.
 * - Fixes an issue where [ModReplayV6] was not applied to scores before version 1.8.4 (see
 * [this](https://github.com/osudroid/osu-droid/commit/4c84a089fa71ecec274b2a62ccb59f52767748d9) commit for more
 * information)
 * - Fixes wrong score multiplier calculation in stacked [ModRateAdjust] mods after version 1.8.4 (see
 * [this](https://github.com/osudroid/osu-droid/commit/0032b1cff542002856f8e4108a0acb4e4aae38ed) commit for more
 * information)
 */
val MIGRATION_2_3 = object : BackedUpMigration(2, 3) {
    override fun performMigration(db: SupportSQLiteDatabase) {
        // Fix score multiplier calculation for stacked ModRateAdjust mods.
        // Score cutoff time - this was when the score multiplier bug was introduced in release (version 1.8.4).
        // Scores before this time are not affected by the bug and do not need to be recalculated.
        val oldScoreCutoffTime = 1752863880000L

        db.query("SELECT id, score, time, mods, beatmapMD5 FROM ScoreInfo").use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                var score = it.getInt(1)
                val time = it.getLong(2)
                val modString = it.getString(3)
                val beatmapMD5 = it.getString(4)

                var mods: ModHashMap

                try {
                    // Check if the mods are already in the new format. In that case, we don't need to migrate them.
                    // Realistically, this should never happen since migrations are done in a transaction, but there are
                    // crash reports where the mods were not migrated in the last migration.
                    mods = ModUtils.deserializeMods(modString)
                } catch (_: Exception) {
                    // If the mods are not deserializable, we assume they are legacy mods, so we migrate them.
                    @Suppress("DuplicatedCode")
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

                    mods = LegacyModConverter.convert(modString, difficulty)
                    mods.put(ModReplayV6())

                    db.execSQL(
                        "UPDATE ScoreInfo SET mods = ? WHERE id = ?",
                        arrayOf<Any>(mods.serializeMods().toString(), id)
                    )
                }

                // Scores with ModReplayV6 are not affected the bug, so we do not need to migrate them.
                if (ModReplayV6::class in mods) {
                    continue
                }

                if (time < oldScoreCutoffTime) {
                    // These scores should have ModReplayV6 applied to them, so we add it.
                    mods.put(ModReplayV6())
                } else {
                    // These scores may be affected by the score multiplier bug, so we need to recalculate the score.
                    val rateAdjustingMods = mods.values.filterIsInstance<ModRateAdjust>()

                    if (rateAdjustingMods.size >= 2) {
                        // Stacked ModRateAdjust mods - recalculate score.
                        val oldScoreMultiplier = rateAdjustingMods.fold(1f) { acc, mod ->
                            acc * mod.scoreMultiplier
                        }

                        val newScoreMultiplier = ModUtils.calculateScoreMultiplier(rateAdjustingMods)

                        score = (score * newScoreMultiplier / oldScoreMultiplier).toInt()
                    }
                }

                db.execSQL(
                    "UPDATE ScoreInfo SET score = ?, mods = ? WHERE id = ?",
                    arrayOf<Any>(score, mods.serializeMods().toString(), id)
                )
            }
        }

        // Add new columns for slider tick and end hits statistics (both are nullable integer columns).
        db.execSQL("ALTER TABLE ScoreInfo ADD COLUMN sliderTickHits INTEGER")
        db.execSQL("ALTER TABLE ScoreInfo ADD COLUMN sliderEndHits INTEGER")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
