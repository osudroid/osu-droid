package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap

/**
 * A parser for parsing a specific beatmap section.
 */
abstract class BeatmapSectionParser {
    protected val maxCoordinateValue = 131072
    private val maxParseLimit = Int.MAX_VALUE

    /**
     * Parses a line.
     *
     * @param beatmap The beatmap to fill.
     * @param line The line to parse.
     */
    abstract fun parse(beatmap: Beatmap, line: String)

    /**
     * Resets this parser into its original state.
     */
    open fun reset() {}

    /**
     * Attempts to parse a string into an integer.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the integer being parsed.
     * @return The parsed integer.
     * @throws NumberFormatException When the resulting value is invalid, or it is out of the parse limit bound.
     */
    @Throws(NumberFormatException::class)
    protected fun parseInt(str: String, parseLimit: Int = maxParseLimit) = str.toInt().also {
        if (it < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (it > parseLimit) {
            throw NumberFormatException("Value is too high")
        }
    }

    /**
     * Attempts to parse a string into a float.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the float being parsed.
     * @param allowNaN Whether to allow NaN.
     * @return The parsed float.
     * @throws NumberFormatException When the resulting value is invalid or out of bounds.
     */
    @JvmOverloads
    @Throws(NumberFormatException::class)
    protected fun parseFloat(
        str: String,
        parseLimit: Float = maxParseLimit.toFloat(),
        allowNaN: Boolean = false
    ) = str.toFloat().also {
        if (it < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (it > parseLimit) {
            throw NumberFormatException("Value is too high")
        }

        if (!allowNaN && it.isNaN()) {
            throw NumberFormatException("Not a number")
        }
    }

    /**
     * Attempts to parse a string into a double.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the double being parsed.
     * @param allowNaN Whether to allow NaN.
     * @return The parsed double.
     * @throws NumberFormatException When the resulting value is invalid or out of bounds.
     */
    @JvmOverloads
    @Throws(NumberFormatException::class)
    protected fun parseDouble(
        str: String,
        parseLimit: Double = maxParseLimit.toDouble(),
        allowNaN: Boolean = false
    ) = str.toDouble().also {
        if (it < -parseLimit) {
            throw NumberFormatException("Value is too low")
        }

        if (it > parseLimit) {
            throw NumberFormatException("Value is too high")
        }

        if (!allowNaN && it.isNaN()) {
            throw NumberFormatException("Not a number")
        }
    }
}
