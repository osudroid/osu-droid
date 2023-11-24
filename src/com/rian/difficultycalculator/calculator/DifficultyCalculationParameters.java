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
     * The custom AR setting to calculate for. Set to <code>Float.NaN</code> to disable.
     */
    public float customAR = Float.NaN;

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
     * Whether custom AR is used in this parameter.
     */
    public boolean isCustomAR() {
        return !Float.isNaN(customAR);
    }

    /**
     * Copies this instance to another instance.
     *
     * @return The copied instance.
     */
    public DifficultyCalculationParameters copy() {
        var copy = new DifficultyCalculationParameters();

        copy.mods = EnumSet.copyOf(mods);
        copy.customAR = customAR;
        copy.customSpeedMultiplier = customSpeedMultiplier;

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof DifficultyCalculationParameters)) {
            return false;
        }

        var other = (DifficultyCalculationParameters) obj;

        if (customSpeedMultiplier != other.customSpeedMultiplier) {
            return false;
        }

        if (isCustomAR() != other.isCustomAR()) {
            return false;
        }

        // If both parameters enable force AR, check for equality.
        if (isCustomAR() && other.isCustomAR() && customAR != other.customAR) {
            return false;
        }

        // Check whether mods are equal.
        return mods.size() == other.mods.size() && mods.containsAll(other.mods);
    }
}
