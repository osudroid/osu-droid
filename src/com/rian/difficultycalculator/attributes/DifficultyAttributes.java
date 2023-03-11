package com.rian.difficultycalculator.attributes;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;

/**
 * Holds data that can be used to calculate performance points.
 */
public abstract class DifficultyAttributes {
    /**
     * The mods which were applied to the beatmap.
     */
    public EnumSet<GameMod> mods;

    /**
     * The combined star rating of all skills.
     */
    public double starRating;

    /**
     * The maximum achievable combo.
     */
    public int maxCombo;

    /**
     * The difficulty corresponding to the aim skill.
     */
    public double aimDifficulty;

    /**
     * The difficulty corresponding to the flashlight skill.
     */
    public double flashlightDifficulty;

    /**
     * The number of clickable objects weighted by difficulty.
     * <br><br>
     * Related to speed/tap difficulty.
     */
    public double speedNoteCount;

    /**
     * Describes how much of aim difficulty is contributed to by hit circles or sliders.
     * <br><br>
     * A value closer to 1 indicates most of aim difficulty is contributed by hit circles.
     * <br><br>
     * A value closer to 0 indicates most of aim difficulty is contributed by sliders.
     */
    public double aimSliderFactor;

    /**
     * The perceived approach rate inclusive of rate-adjusting mods (DT/HT/etc).
     * <br><br>
     * Rate-adjusting mods don't directly affect the approach rate difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    public double approachRate;

    /**
     * The perceived overall difficulty inclusive of rate-adjusting mods (DT/HT/etc), based on osu!standard judgement.
     * <br><br>
     * Rate-adjusting mods don't directly affect the overall difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    public double overallDifficulty;

    /**
     * The number of hit circles in the beatmap.
     */
    public int hitCircleCount;

    /**
     * The number of sliders in the beatmap.
     */
    public int sliderCount;

    /**
     * The number of spinners in the beatmap.
     */
    public int spinnerCount;
}
