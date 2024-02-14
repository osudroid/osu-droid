package ru.nsu.ccfit.zuev.osu.beatmap.constants;

/**
 * Represents available sample banks.
 */
public enum SampleBank {
    none(""),
    normal("normal"),
    soft("soft"),
    drum("drum");

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
        return switch (value) {
            case 1 -> normal;
            case 2 -> soft;
            case 3 -> drum;
            default -> none;
        };
    }

    /**
     * Converts a string value to its sample bank counterpart.
     *
     * @param value The value to convert.
     * @return The sample bank counterpart of the given value.
     */
    public static SampleBank parse(String value) {
        return switch (value) {
            case "Normal" -> normal;
            case "Soft" -> soft;
            case "Drum" -> drum;
            default -> none;
        };
    }
}
