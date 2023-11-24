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
     * The custom CS setting to calculate for. Set to <code>Float.NaN</code> to disable.
     */
    public float customCS = Float.NaN;

    /**
     * The custom AR setting to calculate for. Set to <code>Float.NaN</code> to disable.
     */
    public float customAR = Float.NaN;

    /**
     * The custom OD setting to calculate for. Set to <code>Float.NaN</code> to disable.
     */
    public float customOD = Float.NaN;

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
     * Whether custom CS is used in this parameter.
     */
    public boolean isCustomCS() {
        return !Float.isNaN(customCS);
    }

    /**
     * Whether custom AR is used in this parameter.
     */
    public boolean isCustomAR() {
        return !Float.isNaN(customAR);
    }

    /**
     * Whether custom OD is used in this parameter.
     */
    public boolean isCustomOD() {
        return !Float.isNaN(customOD);
    }

    /**
     * Copies this instance to another instance.
     *
     * @return The copied instance.
     */
    public DifficultyCalculationParameters copy() {
        var copy = new DifficultyCalculationParameters();

        copy.mods = EnumSet.copyOf(mods);
        copy.customCS = customCS;
        copy.customAR = customAR;
        copy.customOD = customOD;
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

        if (
            isCustomCS() != other.isCustomCS() ||
            isCustomAR() != other.isCustomAR() ||
            isCustomOD() != other.isCustomOD()
        ) {
            return false;
        }

        if (isCustomCS() && other.isCustomCS() && customCS != other.customCS) {
            return false;
        }

        if (isCustomAR() && other.isCustomAR() && customAR != other.customAR) {
            return false;
        }

        if (isCustomOD() && other.isCustomOD() && customOD != other.customOD) {
            return false;
        }

        return mods.size() == other.mods.size() && mods.containsAll(other.mods);
    }
}
