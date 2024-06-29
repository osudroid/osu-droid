package com.rian.osu.difficulty.attributes

/**
 * A structure containing the osu!droid performance values of a score.
 */
class DroidPerformanceAttributes : PerformanceAttributes() {
    /**
     * The tap performance value.
     */
    @JvmField
    var tap = 0.0

    /**
     * The visual performance value.
     */
    @JvmField
    var visual = 0.0

    /**
     * The tap penalty used to penalize the tap performance value.
     */
    @JvmField
    var tapPenalty = 1.0

    /**
     * The estimated deviation of the score.
     */
    @JvmField
    var deviation = 0.0

    /**
     * The estimated tap deviation of the score.
     */
    @JvmField
    var tapDeviation = 0.0

    /**
     * The penalty used to penalize the aim performance value.
     */
    @JvmField
    var aimSliderCheesePenalty = 1.0

    /**
     * The penalty used to penalize the flashlight performance value.
     */
    @JvmField
    var flashlightSliderCheesePenalty = 1.0

    /**
     * The penalty used to penalize the visual performance value.
     */
    @JvmField
    var visualSliderPerformancePenalty = 1.0
}