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
    public double customSpeedMultiplier = 1;

    /**
     * The forced AR setting to calculate for. Set to <code>Double.NaN</code> to disable.
     */
    public double forcedAR = Double.NaN;

    /**
     * Retrieves the overall speed multiplier to calculate for.
     */
    public double getTotalSpeedMultiplier() {
        double speedMultiplier = customSpeedMultiplier;

        if (mods.contains(GameMod.MOD_DOUBLETIME) || mods.contains(GameMod.MOD_NIGHTCORE)) {
            speedMultiplier *= 1.5;
        }

        if (mods.contains(GameMod.MOD_HALFTIME)) {
            speedMultiplier *= 0.75;
        }

        return speedMultiplier;
    }

    /**
     * Whether force AR is used in this parameter.
     */
    public boolean isForceAR() {
        return !Double.isNaN(forcedAR);
    }
}
