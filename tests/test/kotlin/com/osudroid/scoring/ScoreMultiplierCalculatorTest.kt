package com.osudroid.scoring

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.*
import org.junit.Assert
import org.junit.Test

class ScoreMultiplierCalculatorTest {
    @Test
    fun `No mods yields 1`() {
        Assert.assertEquals(1.0, calculateMultiplier(emptyList()), 1e-6)
    }

    @Test
    fun `Flat multiplier`() {
        Assert.assertEquals(0.15, calculateMultiplier(listOf(ModEasy())), 1e-6)
    }

    @Test
    fun `Setting-dependent multiplier`() {
        // Default HT rate = 0.75
        // (1 + 0.75) / 4 = 0.4375
        Assert.assertEquals(
            0.4375, calculateMultiplier(listOf(ModHalfTime())), 1e-6
        )
    }

    @Test
    fun `Difficulty-dependent multiplier`() {
        val calculator = TestScoreMultiplierCalculator(BeatmapDifficulty(od = 0f))

        Assert.assertEquals(0.1, calculator.calculateFor(listOf(ModEasy())), 1e-6)
    }

    @Test
    fun `Combination multiplier replaces individual singles`() {
        Assert.assertEquals(
            0.003, calculateMultiplier(listOf(ModEasy(), ModHalfTime())), 1e-6
        )
    }

    @Test
    fun `Combination and flat multipliers`() {
        // Easy+HT: combination (0.003)
        // HR: flat single (1.4)
        // product = 1.4 * 0.003 = 0.0042
        Assert.assertEquals(
            0.003 * 1.4,
            calculateMultiplier(listOf(ModEasy(), ModHalfTime(), ModHardRock())),
            1e-6
        )
    }

    @Test
    fun `Group multiplier collects all matching mods`() {
        // group<ModRateAdjust>: HT (0.75) * DT (1.5) = 1.125 combined rate
        // Group result is combined rate itself
        Assert.assertEquals(
            0.75 * 1.5,
            calculateGroupMultiplier(listOf(ModHalfTime(), ModDoubleTime())),
            1e-6
        )
    }

    @Test
    fun `Group multiplier with single mod`() {
        // Only HT present
        // Group receives [HT], combined rate = 0.75
        Assert.assertEquals(0.75, calculateGroupMultiplier(listOf(ModHalfTime())), 1e-6)
    }

    private fun calculateMultiplier(mods: Iterable<Mod>, difficulty: BeatmapDifficulty? = null) =
        TestScoreMultiplierCalculator(difficulty).calculateFor(mods)

    private fun calculateGroupMultiplier(mods: Iterable<Mod>) =
        TestGroupScoreMultiplierCalculator().calculateFor(mods)

    private class TestScoreMultiplierCalculator(difficulty: BeatmapDifficulty? = null) :
        BaseScoreMultiplierCalculator<Double>(difficulty) {

        init {
            // Flat constant; doubles as difficulty-dependent via construction-time check.
            single<ModEasy>(if (difficulty?.od == 0f) 0.1 else 0.15)
            // Setting-dependent: (1 + rate) / 4.
            // At default HT (0.75x) = 0.4375.
            single<ModHalfTime> { (1.0 + trackRateMultiplier) / 4.0 }
            // Flat constant.
            single<ModHardRock>(1.4)
            // Combination: replaces both singles when both are present.
            combination<ModEasy, ModHalfTime> { _, _ -> 0.003 }
        }

        override val defaultMultiplier = 1.0
        override fun multiply(a: Double, b: Double) = a * b
    }

    // Returns the product of all ModRateAdjust.trackRateMultiplier values as the multiplier.
    private class TestGroupScoreMultiplierCalculator : BaseScoreMultiplierCalculator<Double>() {
        init {
            group<ModRateAdjust> { fold(1.0) { acc, mod -> acc * mod.trackRateMultiplier } }
        }

        override val defaultMultiplier = 1.0
        override fun multiply(a: Double, b: Double) = a * b
    }
}
