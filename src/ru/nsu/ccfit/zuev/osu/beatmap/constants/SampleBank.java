package ru.nsu.ccfit.zuev.osu.beatmap.constants;

/**
 * Represents available sample banks.
 */
public enum SampleBank {
    none(), normal(), soft(), drum();

    SampleBank() {
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
