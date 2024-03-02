package ru.nsu.ccfit.zuev.osu;

/**
 * Represents the algorithm used to calculate the difficulty of a beatmap.
 */
public enum DifficultyAlgorithm {
    /**
     * osu!droid algorithm.
     */
    droid,

    /**
     * osu!standard algorithm.
     */
    standard,

    /**
     * Both osu!droid and osu!standard algorithms.
     */
    both;

    /**
     * Parses an integer value to a {@link DifficultyAlgorithm}.
     *
     * @param value The integer value to parse.
     * @return The parsed {@link DifficultyAlgorithm}.
     */
    public static DifficultyAlgorithm parse(int value) {
        return switch (value) {
            case 1 -> standard;
            case 2 -> both;
            default -> droid;
        };
    }
}
