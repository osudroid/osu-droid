package com.rian.osu.beatmap.sections

import kotlin.math.sign

/**
 * Contains difficulty settings of a beatmap.
 */
class BeatmapDifficulty @JvmOverloads constructor(
    /**
     * The circle size of this beatmap.
     */
    cs: Float = 5f,

    /**
     * The approach rate of this beatmap.
     */
    ar: Float? = null,

    /**
     * The overall difficulty of this beatmap.
     */
    @JvmField
    var od: Float = 5f,

    /**
     * The health drain rate of this beatmap.
     */
    @JvmField
    var hp: Float = 5f
) : Cloneable {
    /**
     * The circle size of this beatmap in difficulty calculation.
     *
     * The game calculates circle size by using the currently running device's height in a way such that it is
     * possible to get varying difficulty calculation results under the same mod configuration across different
     * devices. As such, in difficulty calculation the height of the device is assumed to be a fixed value.
     * For this reason, this circle size measurement should **not** be used in gameplay unless necessary.
     *
     * For circle size in gameplay, see [gameplayCS].
     */
    @JvmField
    var difficultyCS = cs

    /**
     * The circle size of this beatmap in gameplay.
     *
     * This may differ from [difficultyCS], as the game calculates circle size by using the currently running
     * device's height in a way such that it is possible to get varying difficulty calculation results under the
     * same mod configuration across different devices. As such, in difficulty calculation the height of the
     * device is assumed to be a fixed value.
     *
     * Unlike [difficultyCS], this circle size measurement uses the device's height, and should be used in gameplay.
     */
    @JvmField
    var gameplayCS = cs

    /**
     * The approach rate of this beatmap.
     */
    @get:JvmName("getAR")
    @set:JvmName("setAR")
    var ar = ar ?: Float.NaN
        get() = field.takeUnless { it.isNaN() } ?: od

    /**
     * The base slider velocity in hundreds of osu! pixels per beat.
     */
    @JvmField
    var sliderMultiplier = 1.0

    /**
     * The amount of slider ticks per beat.
     */
    @JvmField
    var sliderTickRate = 1.0

    fun apply(other: BeatmapDifficulty) = run {
        difficultyCS = other.difficultyCS
        gameplayCS = other.gameplayCS
        ar = other.ar
        od = other.od
        hp = other.hp
        sliderMultiplier = other.sliderMultiplier
        sliderTickRate = other.sliderTickRate
    }

    public override fun clone() = super.clone() as BeatmapDifficulty

    companion object {
        /**
         * Maps a difficulty value [0, 10] to a two-piece linear range of values.
         *
         * @param difficulty The difficulty value to be mapped.
         * @param min Minimum of the resulting range which will be achieved by a difficulty value of 0.
         * @param mid Midpoint of the resulting range which will be achieved by a difficulty value of 5.
         * @param max Maximum of the resulting range which will be achieved by a difficulty value of 10.
         */
        @JvmStatic
        fun difficultyRange(difficulty: Double, min: Double, mid: Double, max: Double) =
            when {
                difficulty > 5 -> mid + (max - mid) * (difficulty - 5) / 5
                difficulty < 5 -> mid + (mid - min) * (difficulty - 5) / 5
                else -> mid
            }

        /**
         * Inverse function to [difficultyRange]. Maps a value returned by the function back to the
         * difficulty that produced it.
         *
         * @param difficultyValue The difficulty-dependent value to be unmapped.
         * @param diff0 Minimum of the resulting range which will be achieved by a difficulty value of 0.
         * @param diff5 Midpoint of the resulting range which will be achieved by a difficulty value of 5.
         * @param diff10 Maximum of the resulting range which will be achieved by a difficulty value of 10.
         * @return The value to which the difficulty value maps in the specified range.
         */
        @JvmStatic
        fun inverseDifficultyRange(difficultyValue: Double, diff0: Double, diff5: Double, diff10: Double) =
            if (sign(difficultyValue - diff5) == sign(diff10 - diff0)) {
                (difficultyValue - diff5) / (diff10 - diff5) * 5 + 5
            } else {
                (difficultyValue - diff5) / (diff5 - diff0) * 5 + 5
            }
    }
}
