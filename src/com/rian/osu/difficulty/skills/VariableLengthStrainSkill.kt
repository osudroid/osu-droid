package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.mods.Mod
import com.rian.util.addInPlace
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Represents a queued strain where the first value is the strain value and the second value is the time at which the
 * strain occurs.
 */
private typealias QueuedStrain = Pair<Double, Double>

/**
 * A skill that evaluates strain over a variable length of time. A new strain peak is created for every [TObject].
 */

abstract class VariableLengthStrainSkill<TObject : DifficultyHitObject>(mods: Iterable<Mod>) : Skill<TObject>(mods) {
    /**
     * The weight by which each strain value decays.
     */
    protected open val decayWeight = 0.9

    /**
     * The maximum length of a strain section, in milliseconds.
     */
    protected open val maxSectionLength = 400.0

    /**
     * The number of [maxSectionLength] sections calculated such that enough of the difficulty value is preserved.
     *
     * This should be overridden if strains are ever used outside [difficultyValue], or if [difficultyValue] is
     * overridden to not use the default geometric sum.
     *
     * This should be removed in the future when a better memory-saving technique is implemented.
     */
    protected val maxStoredSections
        get() = 11 / (1 - decayWeight)

    private var currentSectionPeak = 0.0
    private var currentSectionBegin = 0.0
    private var currentSectionEnd = 0.0
    private var totalLength = 0.0

    private val queuedStrains = ArrayDeque<QueuedStrain>()
    private val strainPeaks = mutableListOf<StrainPeak>()

    /**
     * The live strain peaks for each [maxSectionLength] of the beatmap, including the peak of the current section.
     */
    val currentStrainPeaks
        get() = strainPeaks.toMutableList().apply {
            add(StrainPeak(currentSectionPeak, currentSectionEnd - currentSectionBegin))
        }

    override fun difficultyValue(): Double {
        // Sections with 0 strain are excluded to avoid worst-case time complexity of the following sort (e.g. /b/2351871).
        // These sections will not contribute to the difficulty.
        val strains = currentStrainPeaks.filter { it.value > 0 }.sortedWith { p1, p2 ->
            if (p1.value == p2.value) {
                return@sortedWith p2.sectionLength.compareTo(p1.sectionLength)
            }

            return@sortedWith p2.value.compareTo(p1.value)
        }

        // Time is measured in units of strains.
        var time = 0.0
        var difficulty = 0.0

        for (strain in strains) {
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
            val endTime = time + strain.sectionLength

            val weight = decayWeight.pow(startTime) - decayWeight.pow(endTime)

            difficulty += strain.value * weight
            time = endTime
        }

        return difficulty
    }

    /**
     * Returns the number of strains weighed against the top strain.
     *
     * The result is scaled by clock rate as it affects the total number of strains.
     */
    fun countTopWeightedStrains(difficultyValue: Double): Double {
        if (objectDifficulties.isEmpty()) {
            return 0.0
        }

        // This is what the top strain is if all strain values were identical.
        val consistentTopStrain = difficultyValue * (1 - decayWeight)

        if (consistentTopStrain == 0.0) {
            return objectDifficulties.size.toDouble()
        }

        // Use a weighted sum of all strains.
        return objectDifficulties.sumOf { 1.1 / (1 + exp(-10 * (it / consistentTopStrain - 0.88))) }
    }

    override fun processInternal(current: TObject): Double {
        if (current.index == 0) {
            currentSectionBegin = current.startTime
            currentSectionEnd = currentSectionBegin + maxSectionLength

            currentSectionPeak = strainValueAt(current)

            return currentSectionPeak
        }

        backfillPeaks(current)

        val strain = strainValueAt(current)

        // If the current strain is larger than the current peak, begin a new peak.
        // Otherwise, add it to the queue.
        if (strain > currentSectionPeak) {
            // Clear the queue since none of the strains in there would contribute to difficulty.
            queuedStrains.clear()

            // End the current section with the new peak.
            saveCurrentPeak(current.startTime - currentSectionBegin)

            // Set up the new section to start at the current object with the current strain.
            currentSectionBegin = current.startTime
            currentSectionEnd = currentSectionBegin + maxSectionLength
            currentSectionPeak = strain
        } else {
            // Empty the queue of smaller elements as they would not contribute to difficulty.
            while (queuedStrains.isNotEmpty() && queuedStrains.last().first < strain) {
                queuedStrains.removeLast()
            }

            queuedStrains.add(strain to current.startTime)
        }

        return strain
    }

    /**
     * Calculates the strain value at the [TObject]. This value is calculated with or without respect to
     * previous [TObject]s.
     *
     * @param current The [TObject] for which the strain value should be calculated.
     */
    protected abstract fun strainValueAt(current: TObject): Double

    /**
     * Retrieves the peak strain at a point in time.
     *
     * @param time The time to retrieve the peak strain at.
     * @param current The [TObject].
     * @return The peak strain.
     */
    protected abstract fun calculateInitialStrain(time: Double, current: TObject): Double

    /**
     * Fills the space between the end of the current section and the current [TObject], if any.
     *
     * @param current The current [TObject].
     */
    private fun backfillPeaks(current: TObject) {
        // If the current object starts after the current section ends, we want to start a new section without any harsh drop-off.
        // If we have previous strains that influence the current difficulty, we will prioritize those first.
        // Otherwise, start with the current object's initial strain.
        while (current.startTime > currentSectionEnd) {
            // Save the current peak, marking the end of the section.
            saveCurrentPeak(currentSectionEnd - currentSectionBegin)

            currentSectionBegin = currentSectionEnd

            // If we have queued strains, use those until the object falls into the new section.
            if (queuedStrains.isNotEmpty()) {
                val (strainValue, startTime) = queuedStrains.removeFirst()

                // We want the section to end `maxSectionLength` after the strain we are using as an influence.
                // This means the queued strain will exist in its own section if the gap between it and the object is large enough.
                // This ensures there's no harsh difficulty difference between 2 sections if such a gap exists.
                currentSectionEnd = startTime + maxSectionLength
                startNewSectionFrom(currentSectionBegin, current)

                // If the current object's peak was higher, we do not want to override it with a lower strain.
                // Only use the queued strain if it contributes more difficulty.
                currentSectionPeak = max(currentSectionPeak, strainValue)
            } else {
                // If the queue is empty, we should start the section from the object instead.
                // The queue can be empty if we are starting off of the back of a new peak, or if we drained through all the
                // queue and the object is still later than the section end.
                currentSectionEnd = currentSectionBegin + maxSectionLength
                startNewSectionFrom(currentSectionBegin, current)
            }
        }
    }

    /**
     * Saves the current peak strain level to the list of strain peaks, which will be used to calculate an overall difficulty.
     */
    private fun saveCurrentPeak(sectionLength: Double) {
        strainPeaks.addInPlace(StrainPeak(currentSectionPeak, sectionLength))

        totalLength += sectionLength

        // Remove from the front of our strain peaks if there is any which are too deep to contribute to difficulty.
        // `maxStoredSections` dictates for us how many sections will preserve at least 99.999% of difficulty.
        val maxTotalLength = maxStoredSections * maxSectionLength

        while (totalLength > maxTotalLength) {
            totalLength -= strainPeaks.removeFirst().sectionLength
        }
    }

    private fun startNewSectionFrom(time: Double, current: TObject) {
        // The maximum strain of the new section is not zero by default.
        // This means we need to capture the strain level at the beginning of the new section, and use that as the initial
        // peak level.
        currentSectionPeak = calculateInitialStrain(time, current)
    }
}