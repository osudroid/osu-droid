package com.rian.osu.difficulty.attributes

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.mods.Mod

/**
 * Holds data that can be used to calculate osu!standard performance points.
 */
class StandardDifficultyAttributes : DifficultyAttributes() {
    /**
     * The difficulty corresponding to the speed skill.
     */
    @JvmField
    var speedDifficulty = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the speed skill.
     */
    @JvmField
    var speedDifficultStrainCount = 0.0

    /**
     * Describes how much of [speedDifficultStrainCount] is contributed to by [HitCircle]s or [Slider]s.
     *
     * A value closer to 0 indicates most of [speedDifficultStrainCount] is contributed by [HitCircle]s.
     *
     * A value closer to [Double.POSITIVE_INFINITY] indicates most of [speedDifficultStrainCount] is contributed by [Slider]s.
     */
    @JvmField
    var speedTopWeightedSliderFactor = 0.0

    /**
     * The perceived approach rate inclusive of rate-adjusting [Mod]s (DT/HT/etc.).
     *
     * Rate-adjusting [Mod]s don't directly affect the approach rate difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    @JvmField
    var approachRate = 0.0
}