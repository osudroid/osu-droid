package com.osudroid.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.LegacyModConverter
import com.rian.osu.mods.ModReplayV6
import java.io.IOException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val testDb = "migration-test"
    private val allMigrations = arrayOf(MIGRATION_1_2)

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DroidDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun `Test all migrations`() {
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DroidDatabase::class.java,
            testDb
        ).addMigrations(*allMigrations).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun `Test migration from version 1 to 2`() {
        @Suppress("VariableInitializerIsRedundant")
        var db = helper.createDatabase(testDb, 1).apply {
            // Insert a fake beatmap.
            execSQL(
                "INSERT INTO BeatmapInfo (filename, md5, audioFilename, setDirectory, title, titleUnicode, artist, " +
                "artistUnicode, creator, version, tags, source, dateImported, approachRate, overallDifficulty, " +
                "circleSize, hpDrainRate, bpmMax, bpmMin, mostCommonBPM, length, previewTime, hitCircleCount, " +
                "sliderCount, spinnerCount, maxCombo) VALUES ('', 'md5', '', '', '', '', '', '', '', '', '', '', 0, " +
                "9, 8, 4, 6, 60, 0, 60, 0, 0, 0, 0, 0, 0)"
            )

            val scores = mutableListOf<String>()

            fun addScore(mods: String, md5: String? = null) {
                scores.add("('${md5 ?: "md5"}', '', '', '$mods', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)")
            }

            // A normal score with mods.
            addScore("rhd|x1.10")
            // A score with mods that can be migrated, but with a beatmap that isn't in the database.
            addScore("m", "md5_2")
            // A score with mods that can be migrated and with a beatmap that is in the database.
            addScore("m")

            execSQL(
                "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                "hit300k, hit300, hit100k, hit100, hit50, misses, time) VALUES " +
                scores.joinToString(",")
            )
        }

        db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

        db.query("SELECT id, mods FROM ScoreInfo").use {
            val difficulty = BeatmapDifficulty(4f, 9f, 8f, 6f)

            while (it.moveToNext()) {
                val id = it.getLong(0)
                val mods = it.getString(1)

                // Check if the mods are migrated correctly.
                when (id) {
                    1L -> {
                        val modMap = LegacyModConverter.convert("rhd|x1.10", difficulty)
                        modMap.put(ModReplayV6())

                        Assert.assertEquals(mods, modMap.serializeMods().toString())
                    }

                    2L -> {
                        val modMap = LegacyModConverter.convert("m")
                        modMap.put(ModReplayV6())

                        Assert.assertEquals(mods, modMap.serializeMods().toString())
                    }

                    3L -> {
                        val modMap = LegacyModConverter.convert("m", difficulty)
                        modMap.put(ModReplayV6())

                        Assert.assertEquals(mods, modMap.serializeMods().toString())
                    }

                    else -> throw IllegalStateException("Unknown score ID: $id")
                }
            }
        }
    }
}