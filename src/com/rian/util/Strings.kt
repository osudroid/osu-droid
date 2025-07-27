package com.rian.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Attempts to parse a [String] to a [Float] where the [String] may use a comma as the decimal separator.
 *
 * First, the method will try to use [String.toFloat]. If that fails, it will assume the [String]
 * uses a comma as the decimal separator and will attempt to parse it as such.
 *
 * @return The parsed [Float] value.
 */
fun String.toFloatWithCommaSeparator() = try {
    toFloat()
} catch (_: Exception) {
    // Locale.GERMAN uses comma as decimal separator, so we use it for sample
    val symbols = DecimalFormatSymbols(Locale.GERMAN)
    val format = DecimalFormat("#,##0.00", symbols)

    format.parse(this)!!.toFloat()
}