package com.rian.osu.beatmap.sections

/**
 * Contains difficulty settings of a beatmap.
 */
class BeatmapDifficulty : Cloneable {
    /**
     * The circle size of this beatmap.
     */
    @JvmField
    var cs: Float = 5f

    /**
     * The approach rate of this beatmap.
     */
    var ar = Float.NaN
        get() = field.takeUnless { it.isNaN() } ?: od

    /**
     * The overall difficulty of this beatmap.
     */
    @JvmField
    var od: Float = 5f

    /**
     * The health drain rate of this beatmap.
     */
    @JvmField
    var hp: Float = 5f

    /**
     * The base slider velocity in hundreds of osu! pixels per beat.
     */
    @JvmField
    var sliderMultiplier: Double = 1.0

    /**
     * The amount of slider ticks per beat.
     */
    @JvmField
    var sliderTickRate: Double = 1.0

    fun apply(other: BeatmapDifficulty) = run {
        cs = other.cs
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
                difficulty < 5 -> mid + (max - mid) * (difficulty - 5) / 5
                difficulty > 5 -> mid - (mid - min) * (5 - difficulty) / 5
                else -> mid
            }
    }
}
