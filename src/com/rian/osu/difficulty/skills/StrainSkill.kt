package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Used to processes strain values of [DifficultyHitObject]s, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
abstract class StrainSkill<in TObject : DifficultyHitObject>(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : Skill<TObject>(mods) {
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

    /**
     * All [DifficultyHitObject] strains.
     */
    val objectStrains = mutableListOf<Double>()

    protected var difficulty = 0.0

    private val strainPeaks = mutableListOf<Double>()
    private var currentSectionPeak = 0.0
    private var currentSectionEnd = 0.0
    private val sectionLength = 400

    override fun process(current: TObject) {
        // The first object doesn't generate a strain, so we begin with an incremented section end
        if (current.index == 0) {
            currentSectionEnd = calculateCurrentSectionStart(current)
        }

        while (current.startTime > currentSectionEnd) {
            saveCurrentPeak()
            startNewSectionFrom(currentSectionEnd, current)
            currentSectionEnd += sectionLength
        }

        currentSectionPeak = max(strainValueAt(current), currentSectionPeak)
    }

    /**
     * Returns a list of the peak strains for each [sectionLength] section of the beatmap,
     * including the peak of the current section.
     */
    val currentStrainPeaks
        get() = strainPeaks.toMutableList().apply { add(currentSectionPeak) }

    /**
     * Returns the number of strains weighed against the top strain.
     *
     * The result is scaled by clock rate as it affects the total number of strains.
     */
    fun countDifficultStrains(): Double {
        if (difficulty == 0.0) {
            return 0.0
        }

        // This is what the top strain is if all strain values were identical.
        val consistentTopStrain = difficulty / 10

        // Use a weighted sum of all strains.
        return objectStrains.fold(0.0) { acc, strain ->
            acc + 1.1 / (1 + exp(-10 * (strain / consistentTopStrain - 0.88)))
        }
    }

    /**
     * Reduces the highest strain peaks to account for extreme difficulty spikes based on
     * [reducedSectionCount] and [reducedSectionBaseline].
     *
     * @param strainPeaks The list of strain peaks to reduce.
     */
    protected fun reduceHighestStrainPeaks(strainPeaks: MutableList<Double>) {
        // To avoid sorting operation (which is generally expensive, especially in real-time difficulty
        // calculation), we perform a linear scan to get the highest strain peaks and reduce them that way.
        val highestStrainPeakIndices = IntArray(min(strainPeaks.size, reducedSectionCount)) { -1 }

        if (highestStrainPeakIndices.isEmpty()) {
            return
        }

        for (i in strainPeaks.indices) {
            val strain = strainPeaks[i]

            // Check if the strain fits into the current top strains
            val lowestStrainIndex = highestStrainPeakIndices[highestStrainPeakIndices.size - 1]
            val lowestStrain = if (lowestStrainIndex > -1) strainPeaks[lowestStrainIndex] else 0.0

            if (strain <= lowestStrain) {
                continue
            }

            // Obtain the insertion index of the current strain
            val insertionIndex = highestStrainPeakIndices.indexOfFirst { strain > if (it > -1) strainPeaks[it] else 0.0 }

            // Shift the indices to the right
            for (j in (highestStrainPeakIndices.size - 1) downTo insertionIndex + 1) {
                highestStrainPeakIndices[j] = highestStrainPeakIndices[j - 1]
            }

            // Insert the current strain
            highestStrainPeakIndices[insertionIndex] = i
        }

        for (i in highestStrainPeakIndices.indices) {
            val index = highestStrainPeakIndices[i]

            if (index == -1) {
                continue
            }

            val scale = log10(
                Interpolation.linear(1.0, 10.0, i.toDouble() / reducedSectionCount)
            )

            strainPeaks[index] *= Interpolation.linear(reducedSectionBaseline, 1.0, scale)
        }
    }

    /**
     * Calculates the starting time of a strain section at an object.
     *
     * @param current The object at which the strain section starts.
     * @returns The start time of the strain section.
     */
    protected open fun calculateCurrentSectionStart(current: TObject) =
        ceil(current.startTime / sectionLength) * sectionLength

    /**
     * Calculates the strain value at the hit object.
     * This value is calculated with or without respect to previous objects.
     *
     * @param current The hit object to calculate.
     * @return The strain value at the hit object.
     */
    protected abstract fun strainValueAt(current: TObject): Double

    /**
     * Retrieves the peak strain at a point in time.
     *
     * @param time The time to retrieve the peak strain at.
     * @param current The current hit object.
     * @return The peak strain.
     */
    protected abstract fun calculateInitialStrain(time: Double, current: TObject): Double

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
    private fun startNewSectionFrom(time: Double, current: TObject) {
        // The maximum strain of the new section is not zero by default.
        // This means we need to capture the strain level at the beginning of the new section, and use that as the initial peak level.
        currentSectionPeak = calculateInitialStrain(time, current)
    }
}
