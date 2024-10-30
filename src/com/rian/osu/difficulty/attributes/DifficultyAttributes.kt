package com.rian.osu.difficulty.attributes

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.mods.Mod

/**
 * Holds data that can be used to calculate performance points.
 */
abstract class DifficultyAttributes {
    /**
     * The mods which were applied to the beatmap.
     */
    @JvmField
    var mods = listOf<Mod>()

    /**
     * The combined star rating of all skills.
     */
    @JvmField
    var starRating = 0.0

    /**
     * The maximum achievable combo.
     */
    @JvmField
    var maxCombo = 0

    /**
     * The difficulty corresponding to the aim skill.
     */
    @JvmField
    var aimDifficulty = 0.0

    /**
     * The difficulty corresponding to the flashlight skill.
     */
    @JvmField
    var flashlightDifficulty = 0.0

    /**
     * The number of clickable objects weighted by difficulty.
     *
     * Related to speed difficulty.
     */
    @JvmField
    var speedNoteCount = 0.0

    /**
     * Describes how much of aim difficulty is contributed to by [HitCircle]s or [Slider]s.
     *
     * A value closer to 1 indicates most aim difficulty is contributed by [HitCircle]s.
     *
     * A value closer to 0 indicates most aim difficulty is contributed by [Slider]s.
     */
    @JvmField
    var aimSliderFactor = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the aim skill.
     */
    @JvmField
    var aimDifficultStrainCount = 0.0

    /**
     * The perceived overall difficulty inclusive of rate-adjusting [Mod]s (DT/HT/etc.).
     *
     * Rate-adjusting [Mod]s don't directly affect the overall difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    @JvmField
    var overallDifficulty = 0.0

    /**
     * The number of [HitCircle]s in the beatmap.
     */
    @JvmField
    var hitCircleCount = 0

    /**
     * The number of [Slider]s in the beatmap.
     */
    @JvmField
    var sliderCount = 0

    /**
     * The number of [Spinner]s in the beatmap.
     */
    @JvmField
    var spinnerCount = 0
}
