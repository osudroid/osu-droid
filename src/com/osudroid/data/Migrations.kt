package com.osudroid.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.LegacyModConverter

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
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

                val newMods = LegacyModConverter.convert(oldMods, difficulty)

                db.execSQL("UPDATE ScoreInfo SET mods = ? WHERE id = ?", arrayOf<Any>(newMods.serializeMods().toString(), id))
            }
        }

        db.execSQL("UPDATE BeatmapInfo SET epilepsyWarning = 0")
    }
}