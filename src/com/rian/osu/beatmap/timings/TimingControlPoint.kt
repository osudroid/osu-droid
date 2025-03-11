package com.rian.osu.beatmap.timings

/**
 * Represents a [ControlPoint] that changes a beatmap's BPM and time signature.
 */
class TimingControlPoint(
    /**
     * The time at which this [TimingControlPoint] takes effect, in milliseconds.
     */
    time: Double,

    /**
     * The amount of milliseconds passed for each beat.
     */
    @JvmField
    val msPerBeat: Double,

    /**
     * The time signature at this [TimingControlPoint].
     */
    @JvmField
    val timeSignature: Int
) : ControlPoint(time) {
    /**
     * The BPM at this [TimingControlPoint].
     */
    @JvmField
    val bpm = 60000 / msPerBeat

    // Timing points are never redundant as they can change the time signature.
    override fun isRedundant(existing: ControlPoint) = false
}
