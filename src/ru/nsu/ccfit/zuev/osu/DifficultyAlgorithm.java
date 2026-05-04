package ru.nsu.ccfit.zuev.osu;

import com.osudroid.GameMode;

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
    standard;

    /**
     * Parses an integer value to a {@link DifficultyAlgorithm}.
     *
     * @param value The integer value to parse.
     * @return The parsed {@link DifficultyAlgorithm}.
     */
    public static DifficultyAlgorithm parse(int value) {
        return value == 1 ? standard : droid;
    }

    /**
     * Converts this {@link DifficultyAlgorithm} to its {@link GameMode} equivalent.
     */
    public GameMode toGameMode() {
        return switch (this) {
            case droid -> GameMode.Droid;
            case standard -> GameMode.Standard;
        };
    }
}
