package com.rian.osu.difficultycalculator.calculator

/**
 * A class for specifying parameters for performance calculation.
 */
data class PerformanceCalculationParameters(
    /**
     * The maximum combo achieved.
     */
    @JvmField
    var maxCombo: Int = 0,

    /**
     * The amount of 300 (great) hits achieved.
     */
    @JvmField
    var countGreat: Int = 0,

    /**
     * The amount of 100 (ok) hits achieved.
     */
    @JvmField
    var countOk: Int = 0,

    /**
     * The amount of 50 (meh) hits achieved.
     */
    @JvmField
    var countMeh: Int = 0,

    /**
     * The amount of misses achieved.
     */
    @JvmField
    var countMiss: Int = 0,
) {

}
