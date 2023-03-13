package main.osu.beatmap.constants;

/**
 * Represents the speed of the countdown before the first hit object.
 */
public enum BeatmapCountdown {
    noCountdown(0),
    normal(1),
    half(0.5f),
    twice(2);

    /**
     * The speed at which the beatmap countdown should be played.
     */
    public final float speed;

    /**
     * @param speed The speed at which the beatmap countdown should be played.
     */
    BeatmapCountdown(float speed) {
        this.speed = speed;
    }

    /**
     * Converts a string data from a beatmap file to its enum counterpart.
     *
     * @param data The data to convert.
     * @return The enum representing the data.
     */
    public static BeatmapCountdown parse(final String data) {
        switch (data) {
            case "0":
                return noCountdown;
            case "2":
                return half;
            case "3":
                return twice;
            default:
                return normal;
        }
    }
}

