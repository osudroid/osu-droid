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

        Assert.assertEquals(3, map.size)
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

        Assert.assertEquals(6, map.size)
        Assert.assertTrue(ModDoubleTime::class in map)
        Assert.assertTrue(ModTraceable::class in map)
        Assert.assertTrue(ModNoFail::class in map)

        val customSpeed = map.ofType<ModCustomSpeed>()
        Assert.assertNotNull(customSpeed)
        Assert.assertEquals(1.1f, customSpeed!!.trackRateMultiplier)

        val flashlight = map.ofType<ModFlashlight>()
        Assert.assertNotNull(flashlight)
        Assert.assertEquals(0.24f, flashlight!!.followDelay)

        val difficultyAdjust = map.ofType<ModDifficultyAdjust>()
        Assert.assertNotNull(difficultyAdjust)
        Assert.assertEquals(2.5f, difficultyAdjust!!.cs)
        Assert.assertEquals(7.6f, difficultyAdjust.ar)
        Assert.assertEquals(10.0f, difficultyAdjust.od)
        Assert.assertEquals(5.0f, difficultyAdjust.hp)
    }

    @Test
    fun `Test mod string conversion`() {
        LegacyModConverter.convert("rhd").apply {
            Assert.assertEquals(3, size)
            Assert.assertTrue(ModDoubleTime::class in this)
            Assert.assertTrue(ModHardRock::class in this)
            Assert.assertTrue(ModHidden::class in this)
        }
    }

    @Test
    fun `Test mod string with only extra mod string`() {
        LegacyModConverter.convert("|x1.25").apply {
            Assert.assertEquals(1, size)
            Assert.assertTrue(ModCustomSpeed::class in this)
            Assert.assertEquals(1.25f, ofType<ModCustomSpeed>()!!.trackRateMultiplier, 0f)
        }
    }

    @Test
    fun `Test mod string conversion without migration`() {
        LegacyModConverter.convert("rhdm").apply {
            Assert.assertEquals(4, size)
            Assert.assertTrue(ModDoubleTime::class in this)
            Assert.assertTrue(ModHardRock::class in this)
            Assert.assertTrue(ModHidden::class in this)
            Assert.assertTrue(ModSmallCircle::class in this)
        }
    }

    @Test
    fun `Test mod string conversion with migration`() {
        val difficulty = BeatmapDifficulty(cs = 4f)

        LegacyModConverter.convert("hdm", difficulty).apply {
            Assert.assertEquals(3, size)
            Assert.assertTrue(ModDoubleTime::class in this)
            Assert.assertTrue(ModHidden::class in this)

            val difficultyAdjust = ofType<ModDifficultyAdjust>()
            Assert.assertNotNull(difficultyAdjust)
            Assert.assertEquals(8f, difficultyAdjust!!.cs)
        }
    }
}