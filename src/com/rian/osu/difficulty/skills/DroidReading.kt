package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidReadingEvaluator
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/**
 * Represents the skill required to read every object in the beatmap.
 */
class DroidReading(
    mods: Iterable<Mod>,
    private val clockRate: Double,
    private val hitObjects: List<HitObject>
) : Skill<DroidDifficultyHitObject>(mods) {
    private val noteDifficulties = mutableListOf<Double>()

    private val strainDecayBase = 0.8
    private val skillMultiplier = 2.0

    private var currentNoteDifficulty = 0.0

    private var difficulty = 0.0
    private var noteWeightSum = 0.0

    override fun process(current: DroidDifficultyHitObject) {
        currentNoteDifficulty *= strainDecay(current.deltaTime)
        currentNoteDifficulty += DroidReadingEvaluator.evaluateDifficultyOf(current, clockRate, mods) * skillMultiplier

        noteDifficulties.add(currentNoteDifficulty * current.rhythmMultiplier)
    }

    override fun difficultyValue(): Double {
        if (hitObjects.isEmpty()) {
            return 0.0
        }

        // Notes with 0 difficulty are excluded to avoid worst-case time complexity of the following sort (e.g. /b/2351871).
        // These notes will not contribute to the difficulty.
        val peaks = noteDifficulties.filter { it > 0 }.toMutableList()

        // Start time at first object.
        val reducedDuration = hitObjects[0].startTime / clockRate + 60 * 1000

        // Assume the first few seconds are completely memorized.
        var reducedCount = 0

        for (obj in hitObjects) {
            if (obj.startTime / clockRate > reducedDuration) {
                break
            }

            ++reducedCount
        }

        for (i in 0 until min(peaks.size, reducedCount)) {
            peaks[i] *= log10(Interpolation.linear(1.0, 10.0, (i.toDouble() / reducedCount).coerceIn(0.0, 1.0)))
        }

        // Difficulty is the weighted sum of the highest notes.
        // We're sorting from highest to lowest note.
        peaks.sortDescending()
        difficulty = 0.0
        noteWeightSum = 0.0

        for (i in peaks.indices) {
            // Use a harmonic sum for note which effectively buffs maps with more notes, especially if
            // note difficulties are consistent. Constants are arbitrary and give good values.
            // https://www.desmos.com/calculator/5eb60faf4c
            val weight = (1.0 + 1.0 / (1.0 + i)) / (i.toDouble().pow(0.8) + 1.0 + 1.0 / (1.0 + i))

            if (weight == 0.0) {
                // Shortcut to avoid unnecessary iterations.
                break
            }

            difficulty += peaks[i] * weight
            noteWeightSum += weight
        }

        return difficulty
    }

    /**
     * Returns the number of relevant objects weighted against the top note.
     */
    fun countTopWeightedNotes(): Double {
        if (noteDifficulties.isEmpty() || difficulty == 0.0 || noteWeightSum == 0.0) {
            return 0.0
        }

        // What would the top note be if all note values were identical
        val consistentTopNote = difficulty / noteWeightSum

        // Use a weighted sum of all notes. Constants are arbitrary and give nice values
        return noteDifficulties.fold(0.0) { acc, d ->
            acc + 1.1 / (1 + exp(-5 * (d / consistentTopNote - 1.15)))
        }
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}