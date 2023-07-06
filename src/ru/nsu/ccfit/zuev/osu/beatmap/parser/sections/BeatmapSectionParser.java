package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a specific beatmap section.
 */
public abstract class BeatmapSectionParser {
    protected static final int maxCoordinateValue = 131072;
    private static final int maxParseLimit = Integer.MAX_VALUE;

    /**
     * Parses a line.
     *
     * @param data The beatmap data to fill.
     * @param line The line to parse.
     */
    public abstract void parse(final BeatmapData data, final String line);

    /**
     * Attempts to parse a string into an integer.
     *
     * @param str The string to parse.
     * @return The parsed integer.
     * @throws NumberFormatException When the resulting value is invalid or it is out of the parse limit bound.
     */
    protected int parseInt(String str) throws NumberFormatException {
        return parseInt(str, maxParseLimit);
    }

    /**
     * Attempts to parse a string into an integer.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the integer being parsed.
     * @return The parsed integer.
     * @throws NumberFormatException When the resulting value is invalid or it is out of the parse limit bound.
     */
    protected int parseInt(String str, int parseLimit) throws NumberFormatException {
        int output = Integer.parseInt(str);

        if (output < -parseLimit) {
            throw new NumberFormatException("Value is too low");
        }

        if (output > parseLimit) {
            throw new NumberFormatException("Value is too high");
        }

        return output;
    }

    /**
     * Attempts to parse a string into a float.
     *
     * @param str The string to parse.
     * @return The parsed float.
     * @throws NumberFormatException When the resulting value is invalid, out of bounds, or NaN.
     */
    protected float parseFloat(String str) throws NumberFormatException {
        return parseFloat(str, maxParseLimit, false);
    }

    /**
     * Attempts to parse a string into a float.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the float being parsed.
     * @return The parsed float.
     * @throws NumberFormatException When the resulting value is invalid, out of bounds, or NaN.
     */
    protected float parseFloat(String str, float parseLimit) throws NumberFormatException {
        return parseFloat(str, parseLimit, false);
    }

    /**
     * Attempts to parse a string into a float.
     *
     * @param str The string to parse.
     * @param allowNaN Whether to allow NaN.
     * @return The parsed float.
     * @throws NumberFormatException When the resulting value is invalid or out of bounds.
     */
    protected float parseFloat(String str, boolean allowNaN) throws NumberFormatException {
        return parseFloat(str, maxParseLimit, allowNaN);
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
    protected float parseFloat(String str, float parseLimit, boolean allowNaN) throws NumberFormatException {
        float output = Float.parseFloat(str);

        if (output < -parseLimit) {
            throw new NumberFormatException("Value is too low");
        }

        if (output > parseLimit) {
            throw new NumberFormatException("Value is too high");
        }

        if (!allowNaN && Float.isNaN(output)) {
            throw new NumberFormatException("Not a number");
        }

        return output;
    }

    /**
     * Attempts to parse a string into a double.
     *
     * @param str The string to parse.
     * @return The parsed double.
     * @throws NumberFormatException When the resulting value is invalid, out of bounds, or NaN.
     */
    protected double parseDouble(String str) throws NumberFormatException {
        return parseDouble(str, maxParseLimit, false);
    }

    /**
     * Attempts to parse a string into a double.
     *
     * @param str The string to parse.
     * @param parseLimit The threshold of the double being parsed.
     * @return The parsed double.
     * @throws NumberFormatException When the resulting value is invalid, out of bounds, or NaN.
     */
    protected double parseDouble(String str, double parseLimit) throws NumberFormatException {
        return parseDouble(str, parseLimit, false);
    }

    /**
     * Attempts to parse a string into a double.
     *
     * @param str The string to parse.
     * @param allowNaN Whether to allow NaN.
     * @return The parsed double.
     * @throws NumberFormatException When the resulting value is invalid or out of bounds.
     */
    protected double parseDouble(String str, boolean allowNaN) throws NumberFormatException {
        return parseDouble(str, maxParseLimit, allowNaN);
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
    protected double parseDouble(String str, double parseLimit, boolean allowNaN) throws NumberFormatException {
        double output = Double.parseDouble(str);

        if (output < -parseLimit) {
            throw new NumberFormatException("Value is too low");
        }

        if (output > parseLimit) {
            throw new NumberFormatException("Value is too high");
        }

        if (!allowNaN && Double.isNaN(output)) {
            throw new NumberFormatException("Not a number");
        }

        return output;
    }
}
