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
            Assert.assertEquals(2.5721537633847564, aimDifficulty, 1e-5)
            Assert.assertEquals(1.5594303200510715, speedDifficulty, 1e-5)
            Assert.assertEquals(0.6705985810017276, readingDifficulty, 1e-5)
            Assert.assertEquals(4.485186735436066, starRating, 1e-6)
        }

        calculator.calculate(beatmap, listOf(ModFlashlight())).apply {
            Assert.assertEquals(1.5766363391518863, flashlightDifficulty, 1e-5)
        }
    }
}