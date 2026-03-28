package com.osudroid.utils

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Returns the standard deviation of the elements in this [Collection].
 */
fun Collection<Double>.standardDeviation(): Double {
    if (isEmpty()) {
        return 0.0
    }

    val mean = average()

    return sqrt(sumOf { (it - mean).pow(2) } / size)
}