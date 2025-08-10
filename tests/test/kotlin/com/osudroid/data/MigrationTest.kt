package com.osudroid.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils
import java.io.IOException
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val testDb = "migration-test"

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
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
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

    @Test
    @Throws(IOException::class)
    fun `Test migration from version 2 to 3`() {
        @Suppress("VariableInitializerIsRedundant")
        var db = helper.createDatabase(testDb, 2).apply {
            val scores = mutableListOf<String>()

            fun addScore(mods: ModHashMap, time: Long = 1752863880000L) {
                scores.add("('md5', '', '', '${mods.serializeMods()}', 1000, 0, 0, 0, 0, 0, 0, 0, 0, $time)")
            }

            fun addScore(mods: String) {
                scores.add("('md5', '', '', '${mods}', 1000, 0, 0, 0, 0, 0, 0, 0, 0, 0)")
            }

            // A score with an unmigrated mod. Only the mods should be migrated and nothing else.
            addScore("dm|")

            // A score without stacked ModRateAdjust mods and no ModReplayV6 added.
            addScore(ModHashMap().apply {
                put(ModDoubleTime())
            })

            // A score with stacked ModRateAdjust mods, but before the score date cutoff, so only it should only have
            // ModReplayV6 added.
            addScore(ModHashMap().apply {
                put(ModDoubleTime())
                put(ModCustomSpeed(0.85f))
            }, 0L)

            // A score with stacked ModRateAdjust mods that should have its score migrated and no ModReplayV6 added.
            addScore(ModHashMap().apply {
                put(ModDoubleTime())
                put(ModCustomSpeed(0.85f))
            })

            execSQL(
                "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                        "hit300k, hit300, hit100k, hit100, hit50, misses, time) VALUES " +
                        scores.joinToString(",")
            )
        }

        db = helper.runMigrationsAndValidate(testDb, 3, true, MIGRATION_2_3)

        db.query("SELECT id, score, mods FROM ScoreInfo").use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val score = it.getInt(1)
                val mods = ModUtils.deserializeMods(it.getString(2))

                // Check if the scores are migrated correctly.
                when (id) {
                    1L -> {
                        // Score with an unmigrated mod, should only be migrated with ModReplayV6 added and nothing else.
                        Assert.assertEquals(3, mods.size)
                        Assert.assertTrue(ModDoubleTime::class in mods)
                        Assert.assertTrue(ModSmallCircle::class in mods)
                        Assert.assertTrue(ModReplayV6::class in mods)

                        Assert.assertEquals(1000, score)
                    }

                    2L -> {
                        // No stacked ModRateAdjust mods, score should remain unchanged.
                        Assert.assertEquals(1000, score)
                        Assert.assertEquals(1, mods.size)
                    }

                    3L -> {
                        // Stacked ModRateAdjust mods before score date cutoff, score should remain unchanged.
                        // Only ModReplayV6 should be added.
                        Assert.assertEquals(1000, score)
                        Assert.assertEquals(3, mods.size)
                        Assert.assertTrue(ModReplayV6::class in mods)
                    }

                    4L -> {
                        // Stacked ModRateAdjust mods, score should be recalculated.
                        Assert.assertEquals(1960, score)
                        Assert.assertEquals(2, mods.size)
                    }

                    else -> throw IllegalStateException("Unknown score ID: $id")
                }
            }
        }
    }
}