package com.osudroid.difficulty.calculator

import com.osudroid.GameMode
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModNoFail
import com.osudroid.mods.ModPrecise
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DroidDifficultyCalculatorTest {
    private val calculator = DroidDifficultyCalculator()

    @Test
    fun `Test difficulty adjustment mod retention`() {
        val retainedMods = calculator.retainDifficultyAdjustmentMods(
            listOf(ModDoubleTime(), ModPrecise(), ModNoFail())
        )

        Assert.assertEquals(retainedMods.size, 2)
        Assert.assertTrue(ModDoubleTime() in retainedMods)
        Assert.assertTrue(ModPrecise() in retainedMods)
    }

    @Test
    fun `Test difficulty calculation sample beatmap`() {
        val beatmap =
            BeatmapParser(
                TestResourceManager.getBeatmapFile(
                    "YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]"
                )!!
            ).parse(true, GameMode.Droid)

        calculator.calculate(beatmap).apply {
            // These results are off by a margin from server-side results due to floating point differences.
            Assert.assertEquals(1.8685609988004601, aimDifficulty, 1e-5)
            Assert.assertEquals(1.492734818431125, tapDifficulty, 1e-5)
            Assert.assertEquals(0.5910394950606784, rhythmDifficulty, 1e-5)
            Assert.assertEquals(0.0801694219632239, readingDifficulty, 1e-5)
            Assert.assertEquals(3.5861666096034437, starRating, 1e-6)
        }

        calculator.calculate(beatmap, listOf(ModFlashlight())).apply {
            Assert.assertEquals(1.3406915364179028, flashlightDifficulty, 1e-5)
        }
    }
}