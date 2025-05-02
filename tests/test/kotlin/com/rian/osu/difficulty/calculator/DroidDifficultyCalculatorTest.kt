package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.mods.ModDoubleTime
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
            ).parse(true, GameMode.Droid)!!

        calculator.calculate(beatmap).apply {
            // These results are off by a margin from server-side results due to floating point differences.
            Assert.assertEquals(2.411367164058181, aimDifficulty, 1e-5)
            Assert.assertEquals(tapDifficulty, 1.4928164438079188, 1e-5)
            Assert.assertEquals(rhythmDifficulty, 0.8031331688998974, 1e-5)
            Assert.assertEquals(flashlightDifficulty, 0.0, 1e-5)
            Assert.assertEquals(visualDifficulty, 0.7809831991279115, 1e-5)
            Assert.assertEquals(4.042873079569523, starRating, 1e-6)
        }
    }
}