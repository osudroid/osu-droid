package com.rian.osu.difficulty.calculator

/**
 * A class for specifying parameters for performance calculation.
 */
open class PerformanceCalculationParameters(
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

    /**
     * The amount of slider nested object misses that do not break combo.
     */
    @JvmField
    var nonComboBreakingSliderNestedMisses: Int? = null,

    /**
     * The amount of slider nested object misses that break combo.
     */
    @JvmField
    var comboBreakingSliderNestedMisses: Int? = null
) {
    /**
     * Whether this score uses classic slider calculation.
     */
    val usingClassicSliderCalculation
        get() = nonComboBreakingSliderNestedMisses == null || comboBreakingSliderNestedMisses == null
}