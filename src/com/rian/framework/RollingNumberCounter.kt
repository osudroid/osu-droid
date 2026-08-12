package com.rian.framework

import com.osudroid.math.Interpolation

/**
 * A [RollingCounter] for [Int] values.
 */
class RollingIntCounter(initialValue: Int) : RollingCounter<Int>(initialValue) {
    override fun interpolate(startValue: Int, endValue: Int, progress: Float) =
        Interpolation.linear(startValue.toFloat(), endValue.toFloat(), progress).toInt()
}

/**
 * A [RollingCounter] for [Float] values.
 */
class RollingFloatCounter(initialValue: Float) : RollingCounter<Float>(initialValue) {
    override fun interpolate(startValue: Float, endValue: Float, progress: Float) =
        Interpolation.linear(startValue, endValue, progress)
}

/**
 * A [RollingCounter] for [Double] values.
 */
class RollingDoubleCounter(initialValue: Double) : RollingCounter<Double>(initialValue) {
    override fun interpolate(startValue: Double, endValue: Double, progress: Float) =
        Interpolation.linear(startValue, endValue, progress.toDouble())
}

/**
 * A [RollingCounter] for [Long] values.
 */
class RollingLongCounter(initialValue: Long) : RollingCounter<Long>(initialValue) {
    override fun interpolate(startValue: Long, endValue: Long, progress: Float) =
        Interpolation.linear(startValue.toDouble(), endValue.toDouble(), progress.toDouble()).toLong()
}