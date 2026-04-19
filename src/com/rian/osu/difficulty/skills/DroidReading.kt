package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidReadingEvaluator
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.log10
import kotlin.math.pow

/**
 * Represents the skill required to read every object in the beatmap.
 */
class DroidReading(
    mods: Iterable<Mod>,
    private val clockRate: Double,
    private val hitObjects: List<HitObject>
) : HarmonicSkill<DroidDifficultyHitObject>(mods) {
    private var currentDifficulty = 0.0

    private val skillMultiplier = 2.5
    private val difficultyDecayBase = 0.8

    override fun objectDifficultyOf(current: DroidDifficultyHitObject): Double {
        currentDifficulty *= difficultyDecay(current.deltaTime)
        currentDifficulty += DroidReadingEvaluator.evaluateDifficultyOf(current, mods) * skillMultiplier

        return currentDifficulty
    }

    override fun applyDifficultyTransformation(difficulties: MutableList<Double>) {
        // Assume the first few seconds are completely memorized.
        val reducedNoteCount = calculateReducedNoteCount()

        for (i in difficulties.indices) {
            difficulties[i] *= log10(
                Interpolation.linear(
                    1.0,
                    10.0,
                    (i.toDouble() / reducedNoteCount).coerceIn(0.0, 1.0)
                )
            )
        }
    }

    private fun calculateReducedNoteCount(): Int {
        if (hitObjects.size < 2) {
            return 0
        }

        val reducedDifficultyDuration = 60 * 1000

        // We take the second note to match `createDifficultyHitObjects`
        val firstDifficultyObject = hitObjects[1]

        val reducedDuration = firstDifficultyObject.startTime / clockRate + reducedDifficultyDuration
        var reducedNoteCount = 0

        for (obj in hitObjects) {
            if (obj.startTime / clockRate > reducedDuration) {
                break
            }

            ++reducedNoteCount
        }

        return reducedNoteCount
    }

    private fun difficultyDecay(ms: Double) = difficultyDecayBase.pow(ms / 1000)
}