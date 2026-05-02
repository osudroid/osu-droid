package com.rian.osu.difficulty.attributes

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider

/**
 * Holds data that can be used to calculate osu!droid performance points.
 */
class DroidDifficultyAttributes : DifficultyAttributes() {
    /**
     * The difficulty corresponding to the tap skill.
     */
    @JvmField
    var tapDifficulty = 0.0

    /**
     * The difficulty corresponding to the rhythm skill.
     */
    @JvmField
    var rhythmDifficulty = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the tap skill.
     */
    @JvmField
    var tapDifficultStrainCount = 0.0

    /**
     * Describes how much of [tapDifficultStrainCount] is contributed to by [HitCircle]s or [Slider]s.
     *
     * A value closer to 0 indicates most of [tapDifficultStrainCount] is contributed by [HitCircle]s.
     *
     * A value closer to [Double.POSITIVE_INFINITY] indicates most of [tapDifficultStrainCount] is contributed by [Slider]s.
     */
    @JvmField
    var tapTopWeightedSliderFactor = 0.0

    /**
     * Possible sections at which the player can use three fingers on.
     */
    @JvmField
    var possibleThreeFingeredSections = mutableListOf<HighStrainSection>()

    /**
     * [Slider]s that are considered difficult.
     */
    @JvmField
    var difficultSliders = mutableListOf<DifficultSlider>()

    /**
     * The maximum score obtainable on the beatmap.
     */
    @JvmField
    var maximumScore = 0
}