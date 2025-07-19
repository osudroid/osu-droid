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
    protected var sliderEndsDropped: Int? = null
    protected var sliderTicksMissed: Int? = null
    protected var effectiveMissCount = 0.0

    /**
     * The accuracy of the parameters.
     */
    protected val accuracy
        get() = if (totalHits > 0) (countGreat * 6.0 + countOk * 2 + countMeh) / (totalHits * 6) else 1.0

    /**
     * The total hits that can be done in the beatmap.
     */
    protected val totalHits
        get() = difficultyAttributes.let { it.hitCircleCount + it.sliderCount + it.spinnerCount }

    /**
     * The total imperfect hits that were done.
     */
    protected val totalImperfectHits
        get() = countOk + countMeh + countMiss

    /**
     * The total hits that were successfully done.
     */
    protected val totalSuccessfulHits
        get() = countGreat + countOk + countMeh

    /**
     * Whether this score uses classic slider calculation.
     */
    protected val usingClassicSliderCalculation
        get() = sliderEndsDropped == null || sliderTicksMissed == null

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
            sliderEndsDropped = it.sliderEndsDropped
            sliderTicksMissed = it.sliderTicksMissed
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
        sliderEndsDropped = null
        sliderTicksMissed = null
        effectiveMissCount = 0.0
    }

    private fun calculateEffectiveMissCount() = difficultyAttributes.run {
        var missCount = countMiss.toDouble()

        if (sliderCount > 0) {
            if (usingClassicSliderCalculation) {
                // Consider that full combo is maximum combo minus dropped slider tails since
                // they don't contribute to combo but also don't break it.
                // In classic scores, we can't know the amount of dropped sliders so we estimate
                // to 10% of all sliders in the beatmap.
                val fullComboThreshold = maxCombo - 0.1 * sliderCount

                if (scoreMaxCombo < fullComboThreshold) {
                    missCount = fullComboThreshold / max(1, scoreMaxCombo)
                }

                // In classic scores, there can't be more misses than a sum of all non-perfect judgements.
                missCount = min(missCount, totalImperfectHits.toDouble())
            } else {
                val fullComboThreshold = maxCombo.toDouble() - sliderEndsDropped!!

                if (scoreMaxCombo < fullComboThreshold) {
                    missCount = fullComboThreshold / max(1, scoreMaxCombo)
                }

                // Combine regular misses with tick misses, since tick misses break combo as well.
                missCount = min(missCount, sliderTicksMissed!! + countMiss.toDouble())
            }
        }

        missCount = max(countMiss.toDouble(), missCount)
        missCount = min(totalHits.toDouble(), missCount)

        missCount
    }
}
