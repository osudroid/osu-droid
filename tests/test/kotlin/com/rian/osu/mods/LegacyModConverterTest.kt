@file:Suppress("DEPRECATION")

package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

class LegacyModConverterTest {
    @Test
    fun `Test GameMod conversion`() {
        val map =
            LegacyModConverter.convert(setOf(GameMod.MOD_DOUBLETIME, GameMod.MOD_TRACEABLE, GameMod.MOD_EASY), "")

        Assert.assertTrue(map.size == 3)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModTraceable::class in map)
        Assert.assertTrue(ModEasy::class in map)
    }

    @Test
    fun `Test GameMod conversion with extra string`() {
        val map =
            LegacyModConverter.convert(
                setOf(GameMod.MOD_DOUBLETIME, GameMod.MOD_TRACEABLE, GameMod.MOD_NOFAIL),
                "x1.10|FLD0.24|CS2.5|AR7.6|OD10.0|HP5.0"
            )

        Assert.assertTrue(map.size == 6)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModTraceable::class in map)
        Assert.assertTrue(ModNoFail::class in map)

        val customSpeed = map.ofType<ModCustomSpeed>()
        Assert.assertNotNull(customSpeed)
        Assert.assertTrue(customSpeed!!.trackRateMultiplier == 1.1f)

        val flashlight = map.ofType<ModFlashlight>()
        Assert.assertNotNull(flashlight)
        Assert.assertTrue(flashlight!!.followDelay == 0.24f)

        val difficultyAdjust = map.ofType<ModDifficultyAdjust>()
        Assert.assertNotNull(difficultyAdjust)
        Assert.assertTrue(difficultyAdjust!!.cs == 2.5f)
        Assert.assertTrue(difficultyAdjust.ar == 7.6f)
        Assert.assertTrue(difficultyAdjust.od == 10.0f)
        Assert.assertTrue(difficultyAdjust.hp == 5.0f)
    }

    @Test
    fun `Test mod string conversion`() {
        val map = LegacyModConverter.convert("rhd")

        Assert.assertTrue(map.size == 3)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModHardRock::class in map)
        Assert.assertTrue(ModHidden::class in map)
    }

    @Test
    fun `Test mod string with only extra mod string`() {
        LegacyModConverter.convert("|x1.25").apply {
            Assert.assertEquals(size, 1)
            Assert.assertTrue(ModCustomSpeed::class in this)
            Assert.assertEquals(ofType<ModCustomSpeed>()!!.trackRateMultiplier, 1.25f, 0f)
        }
    }

    @Test
    fun `Test mod string conversion without migration`() {
        val map = LegacyModConverter.convert("rhdm")

        Assert.assertTrue(map.size == 4)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModHardRock::class in map)
        Assert.assertTrue(ModHidden::class in map)
        Assert.assertTrue(ModSmallCircle::class in map)
    }

    @Test
    fun `Test mod string conversion with migration`() {
        val difficulty = BeatmapDifficulty(cs = 4f)
        val map = LegacyModConverter.convert("hdm", difficulty)

        Assert.assertTrue(map.size == 3)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModHidden::class in map)

        val difficultyAdjust = map.ofType<ModDifficultyAdjust>()
        Assert.assertNotNull(difficultyAdjust)
        Assert.assertTrue(difficultyAdjust!!.cs == 8f)
    }
}