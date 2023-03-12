package main.osu.beatmap.parser.sections;

import main.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a specific beatmap section.
 */
public abstract class BeatmapSectionParser {
    /**
     * Parses a line.
     *
     * @param data The beatmap data to fill.
     * @param line The line to parse.
     * @return Whether the line was successfully parsed.
     */
    public abstract boolean parse(final BeatmapData data, final String line);
}
