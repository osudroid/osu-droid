package main.osu.beatmap.constants;

/**
 * Available sections in a <code>.osu</code> beatmap file.
 */
public enum BeatmapSection {
    general,
    editor,
    metadata,
    difficulty,
    events,
    timingPoints,
    colors,
    hitObjects;

    /**
     * Converts a string section value from a beatmap file to its enum counterpart.
     *
     * @param value The value to convert.
     * @return The enum representing the value.
     */
    public static BeatmapSection parse(final String value) {
        switch (value) {
            case "General":
                return general;
            case "Editor":
                return editor;
            case "Metadata":
                return metadata;
            case "Difficulty":
                return difficulty;
            case "Events":
                return events;
            case "TimingPoints":
                return timingPoints;
            case "Colours":
                return colors;
            case "HitObjects":
                return hitObjects;
            default:
                return null;
        }
    }
}
