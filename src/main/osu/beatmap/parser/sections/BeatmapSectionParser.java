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
     */
    public abstract void parse(final BeatmapData data, final String line);
}
