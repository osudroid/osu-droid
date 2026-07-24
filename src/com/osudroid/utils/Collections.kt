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

/**
 * Returns the median of the elements in this [Collection]. If the collection is empty, returns 0.
 */
fun Collection<Double>.median(): Double {
    if (isEmpty()) {
        return 0.0
    }

    val sorted = sorted()
    val center = sorted.size / 2

    // Use average of the 2 central values if the length is even.
    return if (sorted.size % 2 == 0) (sorted[center - 1] + sorted[center]) / 2 else sorted[center]
}