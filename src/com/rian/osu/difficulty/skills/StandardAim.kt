package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardAgilityEvaluator
import com.rian.osu.difficulty.evaluators.StandardFlowAimEvaluator
import com.rian.osu.difficulty.evaluators.StandardSnapAimEvaluator
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModRelax
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Represents the skill required to correctly aim at every object in the map with a uniform circle size and normalized distances.
 */
class StandardAim(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>,

    /**
     * Whether to consider sliders in the calculation.
     */
    @JvmField
    val withSliders: Boolean
) : VariableLengthStrainSkill<StandardDifficultyHitObject>(mods) {
    private var currentStrain = 0.0

    private val skillMultiplierSnap = 71.0
    private val skillMultiplierAgility = 2.35
    private val skillMultiplierFlow = 245
    private val skillMultiplierTotal = 1.12
    private val combinedSnapMeanExponent = 1.2

    private val reducedSectionTime = 4000.0
    private val reducedSectionBaseline = 0.727

    private val sliderStrains = mutableListOf<Double>()
    private var maxSliderStrain = 0.0

    /**
     * Obtains the amount of sliders that are considered difficult in terms of relative strain.
     */
    fun countDifficultSliders(): Double {
        if (sliderStrains.isEmpty() || maxSliderStrain == 0.0) {
            return 0.0
        }

        return sliderStrains.sumOf { 1 / (1 + exp(-(it / maxSliderStrain * 12 - 6))) }
    }

    /**
     * Obtains the amount of sliders that are considered difficult in terms of relative strain, weighted by consistency.
     *
     * @param difficultyValue The final difficulty value.
     */
    fun countTopWeightedSliders(difficultyValue: Double): Double {
        if (sliderStrains.isEmpty() || maxSliderStrain == 0.0) {
            return 0.0
        }

        val consistentTopStrain = difficultyValue * (1 - decayWeight)

        if (consistentTopStrain == 0.0) {
            return 0.0
        }

        return sliderStrains.sumOf {
            DifficultyCalculationUtils.logistic(it / consistentTopStrain, 0.88, 10.0, 1.1)
        }
    }

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        val decay = strainDecay(current.deltaTime)

        val snapDifficulty = StandardSnapAimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplierSnap
        val agilityDifficulty = StandardAgilityEvaluator.evaluateDifficultyOf(current) * skillMultiplierAgility
        val flowDifficulty = StandardFlowAimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplierFlow

        val totalDifficulty = calculateTotalValue(snapDifficulty, agilityDifficulty, flowDifficulty)

        currentStrain *= decay
        currentStrain += totalDifficulty * (1 - decay)

        if (current.obj is Slider) {
            sliderStrains.add(currentStrain)
            maxSliderStrain = max(maxSliderStrain, currentStrain)
        }

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun calculateTotalValue(snapDifficulty: Double, agilityDifficulty: Double, flowDifficulty: Double): Double {
        var currentFlowDifficulty = flowDifficulty

        // We compare flow to combined snap and agility because snap by itself does not have enough difficulty
        // to be above flow on streams. Agility, on the other hand, is supposed to measure the rate of cursor
        // velocity changes while snapping. This means snapping every circle on a stream requires an enormous
        // amount of agility at which point it is easier to flow.
        var combinedSnapDifficulty = DifficultyCalculationUtils.norm(combinedSnapMeanExponent, snapDifficulty, agilityDifficulty)

        val pSnap = calculateSnapFlowProbability(flowDifficulty / combinedSnapDifficulty)
        val pFlow = 1 - pSnap

        if (mods.any { it is ModRelax }) {
            combinedSnapDifficulty *= 0.75
            currentFlowDifficulty *= 0.6
        }

        val totalDifficulty = combinedSnapDifficulty * pSnap + currentFlowDifficulty * pFlow

        return totalDifficulty * skillMultiplierTotal
    }

    /**
     * Converts the ratio of snap to flow into the probability of snapping or flowing.
     *
     * Constraints:
     * - `P(snap) + P(flow) = 1` (the object is always either snapped or flowed)
     * - `P(snap) = f(snap / flow)` and `P(flow) = f(flow/snap)` (i.e., snap and flow are symmetric and
     * reversible). This means `f(x) + f(1/x) = 1`
     * - `0 <= f(x) <= 1` (cannot have negative or greater than 100% probability of snapping or flowing)
     *
     * This logistic function is a solution, which fits nicely with the general idea of interpolation and
     * provides a tuneable constant.
     *
     * @param ratio The ratio.
     * @returns The probability.
     */
    private fun calculateSnapFlowProbability(ratio: Double) = when {
        ratio == 0.0 -> 0.0
        ratio.isNaN() -> 1.0
        else -> DifficultyCalculationUtils.logistic(-7.27 * ln(ratio))
    }

    override fun difficultyValue(): Double {
        var time = 0.0
        var difficulty = 0.0

        for (strain in getReducedStrainPeaks()) {
            /* Weighting function can be thought of as:
                    b
                    ∫ decayWeight^x dx
                    a
                where a = startTime and b = endTime

                Technically, the function below has been slightly modified from the equation above.
                The real function would be
                    double weight = Math.pow(this.decayWeight, startTime) - Math.pow(this.decayWeight, endTime))
                    ...
                    return difficulty / Math.log(1 / this.decayWeight)
                E.g. for a decayWeight of 0.9, we're multiplying by 10 instead of 9.49122...

                This change makes it so that a beatmap composed solely of maxSectionLength chunks will have the exact same value
                when summed in this class and StrainSkill.
                Doing this ensures the relationship between strain values and difficulty values remains the same between the two
                classes.
            */
            val startTime = time
            val endTime = time + strain.sectionLength / maxSectionLength

            val weight = decayWeight.pow(startTime) - decayWeight.pow(endTime)

            difficulty += strain.value * weight
            time = endTime
        }

        return difficulty / (1 - decayWeight)
    }

    private fun getReducedStrainPeaks(): List<StrainPeak> {
        // Sections with 0 strain are excluded to avoid worst-case time complexity of the following sort (e.g. /b/2351871).
        // These sections will not contribute to the difficulty.
        val strainPeaks = currentStrainPeaks.filter { it.value > 0 }.sortedByDescending { it.value }
        val strains = ArrayList<StrainPeak>(strainPeaks)

        var time = 0.0
        // All strains are removed at the end for optimization.
        var strainsToRemove = 0

        // We are reducing the highest strains first to account for extreme difficulty spikes.
        // Strains are split into 20ms chunks to try to mitigate inconsistencies caused by reducing strains.
        val chunkSize = 20.0

        while (strains.size > strainsToRemove && time < reducedSectionTime) {
            val strain = strains[strainsToRemove]
            var addedTime = 0.0

            while (addedTime < strain.sectionLength) {
                val scale = log10(
                    Interpolation.linear(1.0, 10.0, ((time + addedTime) / reducedSectionTime).coerceIn(0.0, 1.0))
                )

                strains.add(StrainPeak(
                    value = strain.value * Interpolation.linear(reducedSectionBaseline, 1.0, scale),
                    sectionLength = min(chunkSize, strain.sectionLength - addedTime)
                ))
            }

            time += strain.sectionLength
            ++strainsToRemove
        }

        strains.subList(0, strainsToRemove).clear()

        return strains.sortedByDescending { it.value }
    }

    private fun strainDecay(ms: Double) = 0.2.pow(ms / 1000)
}
