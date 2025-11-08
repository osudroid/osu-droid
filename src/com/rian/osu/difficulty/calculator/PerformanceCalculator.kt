package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.PerformanceAttributes

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
    /**
     * The maximum combo achieved.
     */
    protected var scoreMaxCombo = 0
        private set

    /**
     * The amount of great hits achieved.
     */
    protected var countGreat = 0
        private set

    /**
     * The amount of ok hits achieved.
     */
    protected var countOk = 0
        private set

    /**
     * The amount of meh hits achieved.
     */
    protected var countMeh = 0
        private set

    /**
     * The amount of misses achieved.
     */
    protected var countMiss = 0
        private set

    /**
     * The amount of slider nested object misses that do not break combo.
     *
     * Will only be accurate if [usingClassicSliderCalculation] is `false`.
     */
    protected var nonComboBreakingSliderNestedMisses = 0
        private set

    /**
     * The amount of slider nested object misses that break combo.
     *
     * Will only be accurate if [usingClassicSliderCalculation] is `false`.
     */
    protected var comboBreakingSliderNestedMisses = 0
        private set

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
    protected var usingClassicSliderCalculation = false
        private set

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

            usingClassicSliderCalculation = it.comboBreakingSliderNestedMisses != null && it.nonComboBreakingSliderNestedMisses != null

            nonComboBreakingSliderNestedMisses = it.nonComboBreakingSliderNestedMisses ?: 0
            comboBreakingSliderNestedMisses = it.comboBreakingSliderNestedMisses ?: 0
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
        usingClassicSliderCalculation = false
        nonComboBreakingSliderNestedMisses = 0
        comboBreakingSliderNestedMisses = 0
    }
}
