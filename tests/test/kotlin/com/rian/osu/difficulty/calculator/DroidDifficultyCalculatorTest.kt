package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModNoFail
import com.rian.osu.mods.ModPrecise
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
            Assert.assertEquals(1.8692560291811793, aimDifficulty, 1e-5)
            Assert.assertEquals(1.4861690081466008, tapDifficulty, 1e-5)
            Assert.assertEquals(0.5515188197970847, rhythmDifficulty, 1e-5)
            Assert.assertEquals(0.0801694219632239, readingDifficulty, 1e-5)
            Assert.assertEquals(3.5819989914995958, starRating, 1e-6)
        }

        calculator.calculate(beatmap, listOf(ModFlashlight())).apply {
            Assert.assertEquals(1.487806440934379, flashlightDifficulty, 1e-5)
        }
    }
}