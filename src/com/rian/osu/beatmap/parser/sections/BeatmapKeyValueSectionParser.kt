package com.rian.osu.beatmap.parser.sections

import android.text.TextUtils
import java.util.*

/**
 * A parser for parsing beatmap sections that store properties in a key-value pair.
 */
abstract class BeatmapKeyValueSectionParser : BeatmapSectionParser() {
    /**
     * Obtains the property of a line.
     *
     * For example, `ApproachRate:9` will be split into `["ApproachRate", "9"]`.
     *
     * Will return `null` in invalid lines.
     *
     * @param line The line.
     */
    protected fun splitProperty(line: String): Pair<String, String>? =
         line.split(COLON_PROPERTY_REGEX)
             .dropLastWhile { it.isEmpty() }
             .toTypedArray()
             .let { s ->
                 if (s.isEmpty()) {
                     return null
                 }

                 Pair(
                     s[0].trim { it <= ' ' },
                     if (s.size > 1)
                         TextUtils.join(":", Arrays.copyOfRange(s, 1, s.size)).trim { it <= ' ' }
                     else ""
                 )
             }
}
