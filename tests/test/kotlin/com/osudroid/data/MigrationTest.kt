package com.osudroid.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.*
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.ModUtils
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
        helper.createDatabase(testDb, 1).apply {
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

        val db = helper.runMigrationsAndValidate(testDb, 2, true, MIGRATION_1_2)

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

                        Assert.assertEquals(modMap.serializeMods(), mods)
                    }

                    2L -> {
                        val modMap = LegacyModConverter.convert("m")
                        modMap.put(ModReplayV6())

                        Assert.assertEquals(modMap.serializeMods(), mods)
                    }

                    3L -> {
                        val modMap = LegacyModConverter.convert("m", difficulty)
                        modMap.put(ModReplayV6())

                        Assert.assertEquals(modMap.serializeMods(), mods)
                    }

                    else -> throw IllegalStateException("Unknown score ID: $id")
                }
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun `Test migration from version 2 to 3`() {
        helper.createDatabase(testDb, 2).apply {
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

        val db = helper.runMigrationsAndValidate(testDb, 3, true, MIGRATION_2_3)

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

    @Test
    @Throws(IOException::class)
    fun `Test migration from version 3 to 4`() {
        helper.createDatabase(testDb, 3).apply {
            // A score without Flashlight mod. This should not be recalculated.
            execSQL(
                "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                "hit300k, hit300, hit100k, hit100, hit50, misses, time, sliderTickHits, sliderEndHits) VALUES ('md5', " +
                "'', '', '', 1000, 0, '', 0, 0, 0, 0, 0, 0, 0, 0, 0)"
            )

            // A score with Flashlight mod in default settings. This should not be recalculated.
            execSQL(
                "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                "hit300k, hit300, hit100k, hit100, hit50, misses, time, sliderTickHits, sliderEndHits) VALUES ('md5', " +
                "'', '', '[{\"acronym\":\"FL\"}]', 1120, 0, '', 0, 0, 0, 0, 0, 0, 0, 0, 0)"
            )

            // A score with Flashlight mod with custom settings. This should be recalculated.
            execSQL(
                "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                "hit300k, hit300, hit100k, hit100, hit50, misses, time, sliderTickHits, sliderEndHits) VALUES ('md5', " +
                "'', '', '[{\"acronym\":\"FL\",\"settings\":{\"areaFollowDelay\":0.24}}]', 1120, 0, '', 0, 0, 0, 0, 0, 0, 0, 0, 0)"
            )
        }

        val db = helper.runMigrationsAndValidate(testDb, 4, true, MIGRATION_3_4)

        db.query("SELECT id, score, sliderHeadHits, sliderRepeatHits FROM ScoreInfo").use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val score = it.getInt(1)

                when (id) {
                    1L -> {
                        // Score without Flashlight mod, should remain unchanged.
                        Assert.assertEquals(1000, score)

                        // Only check the newly added columns in the first row because the others are the same.
                        Assert.assertTrue(it.isNull(2))
                        Assert.assertTrue(it.isNull(3))
                    }

                    2L -> {
                        // Score with default Flashlight mod, should remain unchanged.
                        Assert.assertEquals(1120, score)
                    }

                    3L -> {
                        // Score with custom Flashlight mod, should be recalculated.
                        Assert.assertEquals(1000, score)
                    }
                }
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun `Test migration from version 4 to 5`() {
        val beatmapMD5 = "md5"
        val missingBeatmapMD5 = "md5_missing"

        // Beatmap difficulty used to compute DA mod's score multiplier during migration.
        val difficulty = BeatmapDifficulty(4f, 9f, 8f, 6f)

        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO BeatmapInfo (filename, md5, audioFilename, setDirectory, title, titleUnicode, artist, " +
                "artistUnicode, creator, version, tags, source, dateImported, approachRate, overallDifficulty, " +
                "circleSize, hpDrainRate, bpmMax, bpmMin, mostCommonBPM, length, previewTime, hitCircleCount, " +
                "sliderCount, spinnerCount, maxCombo, epilepsyWarning) VALUES " +
                "('', '$beatmapMD5', '', '', '', '', '', '', '', '', '', '', 0, " +
                "${difficulty.ar}, ${difficulty.od}, ${difficulty.difficultyCS}, ${difficulty.hp}, 60.0, 0.0, 60.0, " +
                "0, 0, 0, 0, 0, 0, 0)"
            )

            val hdMods = ModHashMap().apply { put(ModHidden()) }.serializeMods()

            // Call applyFromBeatmapDifficulty before serialization. This sets CS' default value to 4 while the value is
            // 7, which will allow the custom CS to be serialized.
            val daMods = ModHashMap().apply {
                put(ModDifficultyAdjust(cs = 7f).also { it.applyFromBeatmapDifficulty(difficulty) })
            }.serializeMods()

            fun insertScore(md5: String, mods: String, score: Int) {
                execSQL(
                    "INSERT INTO ScoreInfo (beatmapMD5, playerName, replayFilename, mods, score, maxCombo, mark, " +
                    "hit300k, hit300, hit100k, hit100, hit50, misses, time, sliderHeadHits, sliderTickHits, " +
                    "sliderRepeatHits, sliderEndHits) VALUES " +
                    "('$md5', '', '', '$mods', $score, 0, '', 0, 0, 0, 0, 0, 0, 0, null, null, null, null)"
                )
            }

            // Multiplier = 1.0; effectiveScore = 1000; expected raw = 1000
            insertScore(beatmapMD5, "", 1000)
            // Multiplier = 1.06; effectiveScore = round(1000 * 1.06) = 1060; expected raw = 1000
            insertScore(beatmapMD5, hdMods, 1060)
            // Multiplier = 1 + 0.0075 * (7-4)^1.5 ≈ 1.038971; effectiveScore = round(1000 * 1.038971) = 1039; expected raw = 1000
            insertScore(beatmapMD5, daMods, 1039)
            // DA mod, beatmap absent; sentinel -1
            insertScore(missingBeatmapMD5, daMods, 1039)

            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 5, true, MIGRATION_4_5)

        db.query("SELECT id, score, needsScoreMigration FROM ScoreInfo").use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val score = it.getInt(1)
                val needsScoreMigration = it.getInt(2) != 0

                when (id) {
                    1L -> {
                        Assert.assertEquals("no-mods score unchanged", 1000, score)
                        Assert.assertFalse("no-mods score needs no migration", needsScoreMigration)
                    }
                    2L -> {
                        Assert.assertEquals("HD score divided by 1.06", 1000, score)
                        Assert.assertFalse("HD score needs no migration", needsScoreMigration)
                    }
                    3L -> {
                        Assert.assertEquals("DA score divided using beatmap difficulty", 1000, score)
                        Assert.assertFalse("DA score with beatmap needs no migration", needsScoreMigration)
                    }
                    4L -> {
                        Assert.assertEquals("DA score without beatmap kept as-is", 1039, score)
                        Assert.assertTrue("DA score without beatmap flagged for migration", needsScoreMigration)
                    }
                    else -> throw IllegalStateException("Unknown score ID: $id")
                }
            }
        }
    }
}