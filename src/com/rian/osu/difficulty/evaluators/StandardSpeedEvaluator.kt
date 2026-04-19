package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import kotlin.math.pow

/**
 * An evaluator for calculating osu!standard speed difficulty.
 */
object StandardSpeedEvaluator {
    private const val MIN_SPEED_BONUS = 200.0

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     *
     *  * time between pressing the previous and current object,
     *  * and how easily they can be cheesed.
     *
     * @param current The current object.
     */
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject): Double {
        if (current.obj is Spinner) {
            return 0.0
        }

        var strainTime = current.strainTime

        // Nerf double-tappable doubles.
        val doubletapness = 1 - current.getDoubletapness(current.next(0))

        // Cap deltatime to the OD 300 hitwindow.
        // 0.93 is derived from making sure 260 BPM 1/4 OD8 streams aren't nerfed harshly, whilst 0.92 limits the effect of the cap.
        strainTime /= (strainTime / current.fullGreatWindow / 0.93).coerceIn(0.92, 1.0)

        // speedBonus will be 0.0 for BPM < 200
        var speedBonus = 0.0

        // Add additional scaling bonus for streams/bursts higher than 200bpm
        if (DifficultyCalculationUtils.millisecondsToBPM(strainTime) < MIN_SPEED_BONUS) {
            speedBonus = 0.75 * ((DifficultyCalculationUtils.bpmToMilliseconds(MIN_SPEED_BONUS) - strainTime) / 40).pow(2)
        }

        // Base difficulty with all bonuses
        var difficulty = (1 + speedBonus) * 1000 / strainTime
        difficulty *= highBpmBonus(current.strainTime)

        // Apply penalty if there's doubletappable doubles
        return difficulty * doubletapness
    }

    private fun highBpmBonus(ms: Double) = 1 / (1 - 0.3.pow(ms / 1000))
}
