package com.rian.osu.difficultycalculator.attributes

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*

/**
 * Holds data that can be used to calculate performance points.
 */
class DifficultyAttributes {
    /**
     * The mods which were applied to the beatmap.
     */
    @JvmField
    var mods = EnumSet.noneOf(GameMod::class.java)

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
     * The difficulty corresponding to the speed skill.
     */
    @JvmField
    var speedDifficulty = 0.0

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
     * Describes how much of aim difficulty is contributed to by hit circles or sliders.
     *
     * A value closer to 1 indicates most of the aim difficulty is contributed by hit circles.
     *
     * A value closer to 0 indicates most of the aim difficulty is contributed by sliders.
     */
    @JvmField
    var aimSliderFactor = 0.0

    /**
     * The perceived approach rate inclusive of rate-adjusting mods (DT/HT/etc.).
     *
     * Rate-adjusting mods don't directly affect the approach rate difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    @JvmField
    var approachRate = 0.0

    /**
     * The perceived overall difficulty inclusive of rate-adjusting mods (DT/HT/etc.).
     *
     * Rate-adjusting mods don't directly affect the overall difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    @JvmField
    var overallDifficulty = 0.0

    /**
     * The number of hit circles in the beatmap.
     */
    @JvmField
    var hitCircleCount = 0

    /**
     * The number of sliders in the beatmap.
     */
    @JvmField
    var sliderCount = 0

    /**
     * The number of spinners in the beatmap.
     */
    @JvmField
    var spinnerCount = 0
}
