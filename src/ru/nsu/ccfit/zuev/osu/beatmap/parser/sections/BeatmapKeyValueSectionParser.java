package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import android.text.TextUtils;

import java.util.Arrays;

/**
 * A parser for parsing beatmap sections that store properties in a key-value pair.
 */
public abstract class BeatmapKeyValueSectionParser extends BeatmapSectionParser {

    /**
     * Obtains the property of a line.
     * <br><br>
     * For example, <code>ApproachRate:9</code> will be split into <code>["ApproachRate", "9"]</code>.
     * <br><br>
     * Will return <code>null</code> for invalid lines.
     *
     * @param line The line.
     */
    protected String[] splitProperty(final String line) {
        String[] s = line.split(":");

        return new String[] {s[0].trim(), s.length > 1 ? TextUtils.join(":", Arrays.copyOfRange(s, 1, s.length)).trim() : ""};
    }

}
