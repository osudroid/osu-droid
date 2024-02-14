package ru.nsu.ccfit.zuev.osu.beatmap.constants;

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
        return switch (value) {
            case "General" -> general;
            case "Editor" -> editor;
            case "Metadata" -> metadata;
            case "Difficulty" -> difficulty;
            case "Events" -> events;
            case "TimingPoints" -> timingPoints;
            case "Colours" -> colors;
            case "HitObjects" -> hitObjects;
            default -> null;
        };
    }
}
