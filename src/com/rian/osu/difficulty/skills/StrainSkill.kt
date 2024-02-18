package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Used to processes strain values of [DifficultyHitObject]s, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
abstract class StrainSkill(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : Skill(mods) {
    /**
     * The final multiplier to be applied to the final difficulty value after all other calculations.
     */
    protected open val difficultyMultiplier = 1.06

    /**
     * The weight by which each strain value decays.
     */
    protected open val decayWeight = 0.9

    /**
     * The number of sections with the highest strains, which the peak strain reductions will apply to.
     *
     * This is done in order to decrease their impact on the overall difficulty of the beatmap for this skill.
     */
    protected open val reducedSectionCount = 10

    /**
     * The baseline multiplier applied to the section with the biggest strain.
     */
    protected open val reducedSectionBaseline = 0.75

    private val strainPeaks = mutableListOf<Double>()
    private var currentSectionPeak = 0.0
    private var currentSectionEnd = 0.0
    private val sectionLength = 400

    override fun process(current: DifficultyHitObject) {
        // The first object doesn't generate a strain, so we begin with an incremented section end
        if (current.index == 0) {
            currentSectionEnd = ceil(current.startTime / sectionLength) * sectionLength
        }

        while (current.startTime > currentSectionEnd) {
            saveCurrentPeak()
            startNewSectionFrom(currentSectionEnd, current)
            currentSectionEnd += sectionLength.toDouble()
        }

        currentSectionPeak = max(strainValueAt(current), currentSectionPeak)
        saveToHitObject(current)
    }

    override fun difficultyValue(): Double {
        val strains = currentStrainPeaks

        // Difficulty is the weighted sum of the highest strains from every section.
        // We're sorting from highest to lowest strain.
        strains.sortDescending()

        if (reducedSectionCount > 0) {
            // We are reducing the highest strains first to account for extreme difficulty spikes.
            for (i in 0 until min(strains.size.toDouble(), reducedSectionCount.toDouble()).toInt()) {
                val scale = log10(
                    Interpolation.linear(
                        1.0,
                        10.0,
                        (i.toFloat() / reducedSectionCount).toDouble().coerceIn(0.0, 1.0)
                    )
                )

                strains[i] = strains[i] * Interpolation.linear(reducedSectionBaseline, 1.0, scale)
            }

            strains.sortDescending()
        }

        var difficulty = 0.0
        var weight = 1.0

        for (strain in strains) {
            difficulty += strain * weight
            weight *= decayWeight
        }

        return difficulty * difficultyMultiplier
    }

    val currentStrainPeaks: MutableList<Double>
        /**
         * Returns a list of the peak strains for each [sectionLength] section of the beatmap,
         * including the peak of the current section.
         */
        get() = strainPeaks.toMutableList().apply { add(currentSectionPeak) }

    /**
     * Calculates the strain value at the hit object.
     * This value is calculated with or without respect to previous objects.
     *
     * @param current The hit object to calculate.
     * @return The strain value at the hit object.
     */
    protected abstract fun strainValueAt(current: DifficultyHitObject): Double

    /**
     * Retrieves the peak strain at a point in time.
     *
     * @param time The time to retrieve the peak strain at.
     * @param current The current hit object.
     * @return The peak strain.
     */
    protected abstract fun calculateInitialStrain(time: Double, current: DifficultyHitObject): Double

    /**
     * Saves the current strain to a hit object.
     *
     * @param current The hit object to save to.
     */
    protected abstract fun saveToHitObject(current: DifficultyHitObject)

    /**
     * Saves the current peak strain level to the list of strain peaks,
     * which will be used to calculate an overall difficulty.
     */
    private fun saveCurrentPeak() = strainPeaks.add(currentSectionPeak)

    /**
     * Sets the initial strain level for a new section.
     *
     * @param time The beginning of the new section, in milliseconds.
     * @param current The current hit object.
     */
    private fun startNewSectionFrom(time: Double, current: DifficultyHitObject) {
        // The maximum strain of the new section is not zero by default.
        // This means we need to capture the strain level at the beginning of the new section, and use that as the initial peak level.
        currentSectionPeak = calculateInitialStrain(time, current)
    }
}
