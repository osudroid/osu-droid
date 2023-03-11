package main.osu.beatmap.constants;

/**
 * Represents available sample banks.
 */
public enum SampleBank {
    none,
    normal,
    soft,
    drum;

    /**
     * Converts a string data from a beatmap file to its enum counterpart.
     *
     * @param data The data to convert.
     * @return The enum representing the data.
     */
    public static SampleBank parse(final String data) {
        switch (data) {
            case "Normal":
                return normal;
            case "Soft":
                return soft;
            case "Drum":
                return drum;
            default:
                return none;
        }
    }
}
