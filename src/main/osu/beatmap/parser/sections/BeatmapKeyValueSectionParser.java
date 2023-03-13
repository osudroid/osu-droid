package main.osu.beatmap.parser.sections;

import java.util.Arrays;

/**
 * A parser for parsing beatmap sections that store properties in a key-value pair.
 */
public abstract class BeatmapKeyValueSectionParser extends BeatmapSectionParser {
    /**
     * Obtains the property of a line.
     *
     * For example, <code>ApproachRate:9</code> will be split into <code>["ApproachRate", "9"]</code>.
     *
     * Will return <code>null</code> for invalid lines.
     *
     * @param line The line.
     */
    protected String[] splitProperty(final String line) {
        String[] s = line.split(":");

        if (s.length < 2) {
            return null;
        }

        return new String[] { s[0], String.join(":", Arrays.copyOfRange(s, 1, s.length)) };
    }
}
