package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import kotlinx.coroutines.CoroutineScope

/**
 * A parser for parsing a specific beatmap section.
 */
abstract class BeatmapSectionParser {
    /**
     * Parses a line.
     *
     * @param beatmap The beatmap to fill.
     * @param line The line to parse.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @throws UnsupportedOperationException When the performed operation for the line is not supported.
     */
    @Throws(UnsupportedOperationException::class)
    abstract fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope? = null)

    /**
     * Attempts to parse a string into an integer.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the integer being parsed.
     * @return The parsed integer.
     * @throws NumberFormatException When the resulting value is invalid, or it is out of the parse limit bound.
     */
    @Throws(NumberFormatException::class)
    protected fun parseInt(str: String, parseLimit: Int = MAX_PARSE_LIMIT) = str.toInt().also {
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
        parseLimit: Float = MAX_PARSE_LIMIT.toFloat(),
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
        parseLimit: Double = MAX_PARSE_LIMIT.toDouble(),
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

    companion object {
        @JvmStatic
        protected val COMMA_PROPERTY_REGEX = ",".toRegex()

        @JvmStatic
        protected val COLON_PROPERTY_REGEX = ":".toRegex()

        private const val MAX_PARSE_LIMIT = Int.MAX_VALUE
    }
}
