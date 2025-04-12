package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModNoFail
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StandardDifficultyCalculatorTest {
    private val calculator = StandardDifficultyCalculator()

    @Test
    fun testDifficultyAdjustmentModRetention() {
        val retainedMods = calculator.retainDifficultyAdjustmentMods(
            listOf(ModDoubleTime(), ModFlashlight(), ModNoFail())
        )

        Assert.assertEquals(retainedMods.size, 2)
        Assert.assertTrue(ModDoubleTime() in retainedMods)
        Assert.assertTrue(ModFlashlight() in retainedMods)
    }

    @Test
    fun testDifficultyCalculationSampleBeatmap() {
        val beatmap =
            BeatmapParser(
                TestResourceManager.getBeatmapFile(
                    "YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]"
                )!!
            ).parse(true, GameMode.Droid)!!

        calculator.calculate(beatmap).apply {
            // These results are off by a margin from server-side results due to floating point differences.
            Assert.assertEquals(aimDifficulty, 2.4086917511345836, 1e-5)
            Assert.assertEquals(speedDifficulty, 1.8083767275879148, 1e-5)
            Assert.assertEquals(flashlightDifficulty, 0.0, 1e-5)
            Assert.assertEquals(starRating, 4.521063460612202, 1e-6)
        }
    }
}