package com.rian.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Attempts to parse a [String] to a [Float] where the [String] uses a comma as the decimal separator.
 *
 * If the parsing fails, this method fallbacks to the original [String.toFloat] method.
 *
 * @return The parsed [Float] value.
 */
fun String.toFloatWithCommaSeparator(): Float {
    // Locale.GERMAN uses comma as decimal separator, so we use it for sample
    val symbols = DecimalFormatSymbols(Locale.GERMAN)
    val format = DecimalFormat("#,##0.00", symbols)

    return try {
        format.parse(this)!!.toFloat()
    } catch (_: Exception) {
        toFloat()
    }
}