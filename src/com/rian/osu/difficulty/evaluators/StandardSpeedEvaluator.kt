package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.StandardDifficultyHitObject
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!standard speed difficulty.
 */
object StandardSpeedEvaluator {
    private const val SINGLE_SPACING_THRESHOLD = 125.0 // 1.25 circles distance between centers
    private const val MIN_SPEED_BONUS = 75.0 // 200 1/4 BPM
    private const val DISTANCE_MULTIPLIER = 0.94

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     *
     *  * time between pressing the previous and current object,
     *  * distance between those objects,
     *  * and how easily they can be cheesed.
     *
     * @param current The current object.
     */
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject): Double {
        if (current.obj is Spinner) {
            return 0.0
        }

        val prev = current.previous(0)
        var strainTime = current.strainTime

        // Nerf double-tappable doubles.
        val doubletapness = 1 - current.doubletapness

        // Cap deltatime to the OD 300 hitwindow.
        // 0.93 is derived from making sure 260 BPM 1/4 OD8 streams aren't nerfed harshly, whilst 0.92 limits the effect of the cap.
        strainTime /= (strainTime / current.fullGreatWindow / 0.93).coerceIn(0.92, 1.0)

        // speedBonus will be 0.0 for BPM < 200
        var speedBonus = 0.0

        // Add additional scaling bonus for streams/bursts higher than 200bpm
        if (strainTime < MIN_SPEED_BONUS) {
            speedBonus = 0.75 * ((MIN_SPEED_BONUS - strainTime) / 40).pow(2.0)
        }

        val travelDistance = prev?.travelDistance ?: 0.0

        // Cap distance at single_spacing_threshold
        val distance = min(SINGLE_SPACING_THRESHOLD, travelDistance + current.minimumJumpDistance)

        // Max distance bonus is 1 * `distance_multiplier` at single_spacing_threshold
        val distanceBonus = (distance / SINGLE_SPACING_THRESHOLD).pow(3.95) * DISTANCE_MULTIPLIER

        // Base difficulty with all bonuses
        val difficulty = (1 + speedBonus + distanceBonus) * 1000 / strainTime

        // Apply penalty if there's doubletappable doubles
        return difficulty * doubletapness
    }
}
