package com.rian.osu.difficulty.attributes

/**
 * A structure containing the performance values of a score.
 */
abstract class PerformanceAttributes {
    /**
     * Calculated score performance points.
     */
    @JvmField
    var total = 0.0

    /**
     * The aim performance value.
     */
    @JvmField
    var aim = 0.0

    /**
     * The accuracy performance value.
     */
    @JvmField
    var accuracy = 0.0

    /**
     * The flashlight performance value.
     */
    @JvmField
    var flashlight = 0.0

    /**
     * The amount of misses including slider breaks.
     */
    @JvmField
    var effectiveMissCount = 0.0
}
