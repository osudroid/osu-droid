package com.rian.difficultycalculator.calculator;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A class for specifying parameters for performance calculation.
 */
public class PerformanceCalculationParameters {
    /**
     * The maximum combo achieved.
     */
    public int maxCombo;

    /**
     * The amount of 300 (great) hits achieved.
     */
    public int countGreat;

    /**
     * The amount of 100 (ok) hits achieved.
     */
    public int countOk;

    /**
     * The amount of 50 (meh) hits achieved.
     */
    public int countMeh;

    /**
     * The amount of misses achieved.
     */
    public int countMiss;
}
