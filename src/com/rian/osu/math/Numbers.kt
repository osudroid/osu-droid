@file:JvmName("NumberExt")
package com.rian.osu.math

import androidx.annotation.IntRange
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.round

/**
 * Rounds this [Float] to the specified number of decimal places.
 *
 * Unlike [round], this method uses [BigDecimal] to round the [Float], which allows for more precise rounding, **but is
 * slower than [round]. Only use when precision is important**.
 *
 * @param precision The number of decimal places to round to.
 * @return The rounded [Float], or:
 * - [Float.POSITIVE_INFINITY] if this [Float] is greater than or equal to [Float.MAX_VALUE].
 * - [Float.NEGATIVE_INFINITY] if this [Float] is less than or equal to -[Float.MAX_VALUE].
 * - This [Float] itself if it is NaN or infinite.
 */
fun Float.preciseRoundBy(@IntRange(from = 0) precision: Int) = when {
    precision < 0 -> throw IllegalArgumentException("Precision must be non-negative")
    isNaN() || isInfinite() -> this
    absoluteValue >= Float.MAX_VALUE -> if (compareTo(0) >= 0) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
    else -> toBigDecimal().setScale(precision, RoundingMode.HALF_UP).toFloat()
}

/**
 * Rounds this [Double] to the specified number of decimal places.
 *
 * Unlike [round], this method uses [BigDecimal] to round the [Double], which allows for more precise rounding, **but is
 * slower than [round]. Only use when precision is important**.
 *
 * @param precision The number of decimal places to round to.
 * @return The rounded [Double], or:
 * - [Double.POSITIVE_INFINITY] if this [Double] is greater than or equal to [Double.MAX_VALUE].
 * - [Double.NEGATIVE_INFINITY] if this [Double] is less than or equal to -[Double.MAX_VALUE].
 * - This [Double] itself if it is NaN or infinite.
 */
fun Double.preciseRoundBy(@IntRange(from = 0) precision: Int) = when {
    precision < 0 -> throw IllegalArgumentException("Precision must be non-negative")
    isNaN() || isInfinite() -> this
    absoluteValue >= Double.MAX_VALUE -> if (compareTo(0) >= 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
    else -> toBigDecimal().setScale(precision, RoundingMode.HALF_UP).toDouble()
}