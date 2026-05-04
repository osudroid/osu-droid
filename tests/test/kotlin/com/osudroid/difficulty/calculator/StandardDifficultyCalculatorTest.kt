package com.osudroid.difficulty.calculator

import com.osudroid.GameMode
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModNoFail
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StandardDifficultyCalculatorTest {
    private val calculator = StandardDifficultyCalculator()

    @Test
    fun `Test difficulty adjustment mod retention`() {
        val retainedMods = calculator.retainDifficultyAdjustmentMods(
            listOf(ModDoubleTime(), ModFlashlight(), ModNoFail())
        )

        Assert.assertEquals(retainedMods.size, 2)
        Assert.assertTrue(ModDoubleTime() in retainedMods)
        Assert.assertTrue(ModFlashlight() in retainedMods)
    }

    @Test
    fun `Test difficulty calculation sample beatmap`() {
        val beatmap =
            BeatmapParser(
                TestResourceManager.getBeatmapFile(
                    "YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]"
                )!!
            ).parse(true, GameMode.Standard)

        calculator.calculate(beatmap).apply {
            // These results are off by a margin from server-side results due to floating point differences.
            Assert.assertEquals(2.5071363711640458, aimDifficulty, 1e-5)
            Assert.assertEquals(1.801599146984512, speedDifficulty, 1e-5)
            Assert.assertEquals(4.552663607000551, starRating, 1e-6)
        }

        calculator.calculate(beatmap, listOf(ModFlashlight())).apply {
            Assert.assertEquals(1.5678659774040957, flashlightDifficulty, 1e-5)
        }
    }
}