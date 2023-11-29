package ru.nsu.ccfit.zuev.osu.beatmap.constants;

/**
 * Represents available sample banks.
 */
public enum SampleBank {
    none(""), normal("normal"), soft("soft"), drum("drum");

    /**
     * The prefix of audio files representing this sample bank.
     */
    public final String prefix;

    SampleBank(final String prefix) {
        this.prefix = prefix;
    }

    /**
     * Converts an integer value to its sample bank counterpart.
     *
     * @param value The value to convert.
     * @return The sample bank counterpart of the given value.
     */
    public static SampleBank parse(int value) {
        switch (value) {
            case 1:
                return normal;
            case 2:
                return soft;
            case 3:
                return drum;
            default:
                return none;
        }
    }

    /**
     * Converts a string value to its sample bank counterpart.
     *
     * @param value The value to convert.
     * @return The sample bank counterpart of the given value.
     */
    public static SampleBank parse(String value) {
        switch (value) {
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
