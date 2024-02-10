package com.rian.osu.beatmap.timings

/**
 * Represents a control point that changes a beatmap's BPM.
 */
class TimingControlPoint(
    /**
     * The time at which this control point takes effect, in milliseconds.
     */
    time: Double,

    /**
     * The amount of milliseconds passed for each beat.
     */
    @JvmField
    val msPerBeat: Double,

    /**
     * The time signature at this control point.
     */
    @JvmField
    val timeSignature: Int
) : ControlPoint(time) {
    /**
     * The BPM of this control point.
     */
    val BPM: Double
        get() = 60000 / msPerBeat

    // Timing points are never redundant as they can change the time signature.
    override fun isRedundant(existing: ControlPoint) = false

    override fun clone() = super.clone() as TimingControlPoint
}
