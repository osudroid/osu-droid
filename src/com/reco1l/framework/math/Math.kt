package com.reco1l.framework.math

import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Round a number by given decimals.
 */
fun Double.roundBy(decimals: Int = 1): Double {
    val factor = (10.0).pow(decimals)
    return (this * factor).roundToInt() / factor
}

/**
 * Round a number by given decimals.
 */
fun Float.roundBy(decimals: Int = 1): Float {
    val factor = (10f).pow(decimals)
    return (this * factor).roundToInt() / factor
}
