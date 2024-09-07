package ru.nsu.ccfit.zuev.osu.scoring;

/**
 * Represents the scoring mode used in online beatmap leaderboard.
 */
public enum BeatmapLeaderboardScoringMode {
    SCORE,
    PP;

    /**
     * Parses an integer value to a {@link BeatmapLeaderboardScoringMode}.
     *
     * @param value The integer value to parse.
     * @return The parsed {@link BeatmapLeaderboardScoringMode}.
     */
    public static BeatmapLeaderboardScoringMode parse(int value) {
        return value == 1 ? PP : SCORE;
    }
}
