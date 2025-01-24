package com.rian.osu.difficulty.utils

import androidx.annotation.IntRange
import com.rian.osu.math.Interpolation
import kotlin.math.exp

/**
 * Utilities for difficulty calculation.
 */
object DifficultyCalculationUtils {
    /**
     * Converts a BPM value to milliseconds.
     *
     * @param bpm The BPM value.
     * @param delimiter The denominator of the time signature. Defaults to 4.
     * @return The BPM value in milliseconds.
     */
    @JvmStatic
    @JvmOverloads
    fun bpmToMilliseconds(bpm: Double, @IntRange(from = 1, to = 4) delimiter: Int = 4) =
        60000 / bpm / delimiter

    /**
     * Converts milliseconds to a BPM value.
     *
     * @param milliseconds The milliseconds value.
     * @param delimiter The denominator of the time signature. Defaults to 4.
     * @return The milliseconds value in BPM.
     */
    @JvmStatic
    @JvmOverloads
    fun millisecondsToBPM(milliseconds: Double, @IntRange(from = 1, to = 4) delimiter: Int = 4) =
        60000 / milliseconds * delimiter

    /**
     * Calculates an S-shaped [logistic function](https://en.wikipedia.org/wiki/Logistic_function)
     * with offset at [x].
     *
     * @param x The value to calculate the function for.
     * @param midpointOffset How much the function midpoint is offset from zero [x].
     * @param multiplier The growth rate of the function.
     * @param maxValue Maximum value returnable by the function.
     * @returns The output of the logistic function calculated at [x].
     */
    @JvmStatic
    @JvmOverloads
    fun logistic(x: Double, midpointOffset: Double, multiplier: Double, maxValue: Double = 1.0) =
        maxValue / (1 + exp(-multiplier * (x - midpointOffset)))

    /**
     * Calculates the [smoothstep](https://en.wikipedia.org/wiki/Smoothstep) function at [x].
     *
     * @param x The value to calculate the function for.
     * @param start The [x] value at which the function returns 0.
     * @param end The [x] value at which the function returns 1.
     * @return The output of the smoothstep function calculated at [x].
     */
    @JvmStatic
    fun smoothstep(x: Double, start: Double, end: Double): Double {
        val t = Interpolation.reverseLinear(x, start, end)

        return t * t * (3 - 2 * t)
    }

    /**
     * Calculates the [smootherstep](https://en.wikipedia.org/wiki/Smoothstep#Variations) function at [x].
     *
     * @param x The value to calculate the function for.
     * @param start The [x] value at which the function returns 0.
     * @param end The [x] value at which the function returns 1.
     * @return The output of the smootherstep function calculated at [x].
     */
    @JvmStatic
    fun smootherstep(x: Double, start: Double, end: Double): Double {
        val t = Interpolation.reverseLinear(x, start, end)

        return t * t * t * (t * (t * 6 - 15) + 10)
    }


}