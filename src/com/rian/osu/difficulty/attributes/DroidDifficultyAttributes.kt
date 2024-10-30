package com.rian.osu.difficulty.attributes

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider

/**
 * Holds data that can be used to calculate osu!droid performance points.
 */
class DroidDifficultyAttributes : DifficultyAttributes() {
    /**
     * The custom speed multiplier that was applied to the beatmap.
     */
    @JvmField
    var customSpeedMultiplier = 1f

    /**
     * The overall clock rate that was applied to the beatmap.
     */
    @JvmField
    var clockRate = 1.0

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
     * The difficulty corresponding to the visual skill.
     */
    @JvmField
    var visualDifficulty = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the tap skill.
     */
    @JvmField
    var tapDifficultStrainCount = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the flashlight skill.
     */
    @JvmField
    var flashlightDifficultStrainCount = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the visual skill.
     */
    @JvmField
    var visualDifficultStrainCount = 0.0

    /**
     * The average delta time of speed objects.
     */
    @JvmField
    var averageSpeedDeltaTime = 0.0

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
     * Describes how much of flashlight difficulty is contributed to by [HitCircle]s or [Slider]s.
     *
     * A value closer to 1 indicates most flashlight difficulty is contributed by [HitCircle]s.
     *
     * A value closer to 0 indicates most flashlight difficulty is contributed by [Slider]s.
     */
    @JvmField
    var flashlightSliderFactor = 1.0

    /**
     * Describes how much of visual difficulty is contributed to by [HitCircle]s or [Slider]s.
     *
     * A value closer to 1 indicates most visual difficulty is contributed by [HitCircle]s.
     *
     * A value closer to 0 indicates most visual difficulty is contributed by [Slider]s.
     */
    @JvmField
    var visualSliderFactor = 1.0

    /**
     * Describes how much of tap difficulty is contributed by notes that are "vibroable".
     *
     * A value closer to 1 indicates most tap difficulty is contributed by notes that are not "vibroable".
     *
     * A value closer to 0 indicates most tap difficulty is contributed by notes that are "vibroable".
     */
    @JvmField
    var vibroFactor = 1.0
}