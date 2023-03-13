package com.rian.difficultycalculator.attributes;

/**
 * Holds data that can be used to calculate rimu! performance points.
 */
public class RimuDifficultyAttributes extends DifficultyAttributes {
    /**
     * The difficulty corresponding to the tap skill.
     */
    public double tapDifficulty;

    /**
     * The difficulty corresponding to the rhythm skill.
     */
    public double rhythmDifficulty;

    /**
     * The difficulty corresponding to the visual skill.
     */
    public double visualDifficulty;

    /**
     * The clock rate that was considered.
     */
    public double clockRate;
}
