package com.rian.difficultycalculator.calculator;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A class for specifying parameters for difficulty calculation.
 */
public class DifficultyCalculationParameters {
    /**
     * The mods to calculate for.
     */
    public EnumSet<GameMod> mods = EnumSet.noneOf(GameMod.class);

    /**
     * The custom speed multiplier to calculate for.
     */
    public float customSpeedMultiplier = 1;

    /**
     * The forced AR setting to calculate for. Set to <code>Double.NaN</code> to disable.
     */
    public float forcedAR = Float.NaN;

    /**
     * Retrieves the overall speed multiplier to calculate for.
     */
    public float getTotalSpeedMultiplier() {
        float speedMultiplier = customSpeedMultiplier;

        if (mods.contains(GameMod.MOD_DOUBLETIME) || mods.contains(GameMod.MOD_NIGHTCORE)) {
            speedMultiplier *= 1.5f;
        }

        if (mods.contains(GameMod.MOD_HALFTIME)) {
            speedMultiplier *= 0.75f;
        }

        return speedMultiplier;
    }

    /**
     * Whether force AR is used in this parameter.
     */
    public boolean isForceAR() {
        return !Float.isNaN(forcedAR);
    }

    /**
     * Copies this instance to another instance.
     *
     * @return The copied instance.
     */
    public DifficultyCalculationParameters copy() {
        var copy = new DifficultyCalculationParameters();

        copy.mods = EnumSet.copyOf(mods);
        copy.forcedAR = forcedAR;
        copy.customSpeedMultiplier = customSpeedMultiplier;

        return copy;
    }
}
