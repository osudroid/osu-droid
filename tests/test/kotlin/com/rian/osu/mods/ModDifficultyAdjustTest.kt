package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModDifficultyAdjustTest {
    @Test
    fun `Test beatmap setting override without additional mods`() {
        val difficulty = BeatmapDifficulty()

        ModDifficultyAdjust(cs = 4f, ar = 8f, od = 7f, hp = 6f).applyToDifficulty(GameMode.Droid, difficulty, listOf())

        Assert.assertEquals(4f, difficulty.difficultyCS, 1e-2f)
        Assert.assertEquals(4f, difficulty.gameplayCS, 1e-2f)
        Assert.assertEquals(8f, difficulty.ar, 1e-2f)
        Assert.assertEquals(7f, difficulty.od, 1e-2f)
        Assert.assertEquals(6f, difficulty.hp, 1e-2f)
    }

    @Test
    fun `Test beatmap setting override with additional mods`() {
        val difficulty = BeatmapDifficulty()

        ModDifficultyAdjust(cs = 6f, ar = 6f, od = 6f, hp = 6f).applyToDifficulty(
            GameMode.Droid, difficulty, listOf(ModHardRock(), ModReallyEasy())
        )

        Assert.assertEquals(6f, difficulty.difficultyCS, 1e-2f)
        Assert.assertEquals(6f, difficulty.gameplayCS, 1e-2f)
        Assert.assertEquals(6f, difficulty.ar, 1e-2f)
        Assert.assertEquals(6f, difficulty.od, 1e-2f)
        Assert.assertEquals(6f, difficulty.hp, 1e-2f)
    }

    @Test
    fun `Test AR override with non-1x speed multiplier`() {
        val difficulty = BeatmapDifficulty()

        ModDifficultyAdjust(ar = 9f).applyToDifficulty(GameMode.Droid, difficulty, listOf(ModDoubleTime()))

        Assert.assertEquals(7f, difficulty.ar, 1e-2f)
    }

    @Test
    fun `Test object fade in adjustments with non-1x speed multiplier AR override`() {
        val mods = listOf(ModDoubleTime())
        val difficulty = BeatmapDifficulty()
        val difficultyAdjust = ModDifficultyAdjust(ar = 9f)

        difficultyAdjust.applyToDifficulty(GameMode.Droid, difficulty, mods)

        Slider(
            0.0, Vector2(0), 0, SliderPath(
                SliderPathType.Linear, listOf(Vector2(0), Vector2(256, 0)), 256.0
            ), true, 0, mutableListOf()
        ).apply {
            applyDefaults(BeatmapControlPoints(), difficulty, GameMode.Droid)
            difficultyAdjust.applyToHitObject(GameMode.Droid, this, mods)

            Assert.assertEquals(timePreempt, 900.0, 1e-2)
            Assert.assertEquals(timeFadeIn, 600.0, 1e-2)

            Assert.assertEquals(head.timePreempt, 900.0, 1e-2)
            Assert.assertEquals(head.timeFadeIn, 600.0, 1e-2)

            Assert.assertTrue(nestedHitObjects.size > 2)

            val tick = nestedHitObjects[1]

            Assert.assertEquals(tick.timePreempt, 1094.0, 1e-2)
            Assert.assertEquals(tick.timeFadeIn, 600.0, 1e-2)
        }
    }

    @Test
    fun `Test serialization`() {
        ModDifficultyAdjust().apply {
            serialize().apply {
                Assert.assertNull(optJSONObject("settings"))
            }

            cs = 4f
            ar = 9f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(4f, getDouble("cs").toFloat(), 1e-5f)
                Assert.assertEquals(9f, getDouble("ar").toFloat(), 1e-5f)
                Assert.assertTrue(optDouble("od").isNaN())
                Assert.assertTrue(optDouble("hp").isNaN())
            }

            od = 8f
            hp = 6f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(4f, getDouble("cs").toFloat(), 1e-5f)
                Assert.assertEquals(9f, getDouble("ar").toFloat(), 1e-5f)
                Assert.assertEquals(8f, getDouble("od").toFloat(), 1e-5f)
                Assert.assertEquals(6f, getDouble("hp").toFloat(), 1e-5f)
            }
        }
    }
}