package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.PerformanceAttributes
import kotlin.math.max
import kotlin.math.min

/**
 * A performance calculator for calculating performance points.
 */
abstract class PerformanceCalculator<
    TDiffAttributes : DifficultyAttributes,
    TPerfAttributes : PerformanceAttributes,
    TPerfParameters : PerformanceCalculationParameters
>(
    /**
     * The [DifficultyAttributes] being calculated.
     */
    @JvmField
    val difficultyAttributes: TDiffAttributes
) {
    protected var scoreMaxCombo = 0
    protected var countGreat = 0
    protected var countOk = 0
    protected var countMeh = 0
    protected var countMiss = 0
    protected var effectiveMissCount = 0.0

    /**
     * The accuracy of the parameters.
     */
    protected val accuracy: Double
        get() = (countGreat * 6.0 + countOk * 2 + countMeh) / (totalHits * 6)

    /**
     * The total hits that can be done in the beatmap.
     */
    protected val totalHits: Int
        get() = difficultyAttributes.let { it.hitCircleCount + it.sliderCount + it.spinnerCount }

    /**
     * The total hits that were successfully done.
     */
    protected val totalSuccessfulHits: Int
        get() = countGreat + countOk + countMeh

    /**
     * Calculates the performance value of the [DifficultyAttributes] with the specified parameters.
     *
     * @param parameters The parameters to create the attributes for. If omitted, the [Beatmap] was assumed to be SS.
     * @return The performance attributes for the beatmap relating to the parameters.
     */
    @JvmOverloads
    fun calculate(parameters: TPerfParameters? = null) = run {
        processParameters(parameters)
        createPerformanceAttributes()
    }

    /**
     * Creates the [PerformanceAttributes] of the [DifficultyAttributes].
     *
     * @return The [PerformanceAttributes] for the [Beatmap] relating to the parameters.
     */
    protected abstract fun createPerformanceAttributes(): TPerfAttributes

    protected open fun processParameters(parameters: TPerfParameters?) =
        parameters?.let {
            scoreMaxCombo = it.maxCombo
            countGreat = it.countGreat
            countOk = it.countOk
            countMeh = it.countMeh
            countMiss = it.countMiss
            effectiveMissCount = calculateEffectiveMissCount()
        } ?: resetDefaults()

    /**
     * Resets this calculator to its original state.
     */
    protected open fun resetDefaults() {
        scoreMaxCombo = difficultyAttributes.maxCombo
        countGreat = totalHits
        countOk = 0
        countMeh = 0
        countMiss = 0
        effectiveMissCount = 0.0
    }

    private fun calculateEffectiveMissCount() = difficultyAttributes.run {
        // Guess the number of misses + slider breaks from combo
        var comboBasedMissCount = 0.0

        if (sliderCount > 0) {
            val fullComboThreshold: Double = maxCombo - 0.1 * sliderCount
            if (scoreMaxCombo < fullComboThreshold) {
                // Clamp miss count to maximum amount of possible breaks.
                comboBasedMissCount = min(
                    fullComboThreshold / max(1, scoreMaxCombo),
                    (countOk + countMeh + countMiss).toDouble()
                )
            }
        }

        max(countMiss.toDouble(), comboBasedMissCount)
    }
}
