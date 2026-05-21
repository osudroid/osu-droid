package com.osudroid.difficulty.utils

import com.osudroid.beatmaps.PlayableBeatmap
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.utils.ModUtils
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object DroidScoreUtils {
    /**
     * Calculates the maximum possible spinner bonus for a [PlayableBeatmap]. Only works for osu!droid but is typed
     * as [PlayableBeatmap] to support other subtypes (i.e., live calculation).
     *
     * @param beatmap The [PlayableBeatmap] to calculate the maximum spinner bonus for.
     * @returns The maximum spinner bonus.
     */
    @JvmStatic
    fun calculateMaximumSpinnerBonus(beatmap: PlayableBeatmap): Int {
        val hitObjects = beatmap.hitObjects

        if (hitObjects.spinnerCount == 0) {
            return 0
        }

        val scoreMultiplier = ModUtils.calculateScoreMultiplier(beatmap.mods)
        var bonus = 0

        // In reality, there is no time-based limit to spinner RPM, since the limit is π/2 rad/*frame* and not rad/second.
        // For the purpose of this calculation, we assume that the frame rate is 120 FPS.
        // For scores that were set in a higher refresh rate, this estimation will underestimate the actual maximum spinner bonus.
        val maximumRotationsPerSecond = PI / 2 * 120
        val minimumRotationsPerSecond = 2 + 2 * beatmap.difficulty.od / 10

        for (obj in hitObjects) {
            if (obj !is Spinner) {
                continue
            }

            val duration = obj.duration / 1000
            val spinsRequiredBeforeBonus = duration * minimumRotationsPerSecond
            val totalPossibleSpins = duration * maximumRotationsPerSecond

            // Spinner bonus points are awarded for each full rotation every nth spin after the required spins.
            // For example, if a spinner requires 5.6 spins before bonus, the first bonus will be awarded at 6 spins
            // instead of 6.6 spins.
            val maximumPossibleBonusSpins =
                max(0.0, floor(totalPossibleSpins) - ceil(spinsRequiredBeforeBonus)).toInt()

            bonus += maximumPossibleBonusSpins * 1000
        }

        return (bonus * scoreMultiplier).toInt()
    }
}