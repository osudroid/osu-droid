package com.rian.osu.beatmap.timings

import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo
import com.rian.osu.beatmap.hitobject.FileHitSampleInfo
import com.rian.osu.beatmap.hitobject.HitSampleInfo

/**
 * Represents a [ControlPoint] that handles sound samples.
 */
class SampleControlPoint(
    /**
     * The time at which this [SampleControlPoint] takes effect, in milliseconds.
     */
    time: Double,

    /**
     * The sample bank at this [SampleControlPoint].
     */
    @JvmField
    val sampleBank: SampleBank,

    /**
     * The sample volume at this [SampleControlPoint].
     */
    @JvmField
    val sampleVolume: Int,

    /**
     * The index of the sample bank, if this sample bank uses custom samples.
     *
     * If this is 0, the beatmap's sample should be used instead.
     */
    @JvmField
    val customSampleBank: Int
): ControlPoint(time) {
    override fun isRedundant(existing: ControlPoint) =
        existing is SampleControlPoint &&
        sampleBank == existing.sampleBank &&
        sampleVolume == existing.sampleVolume &&
        customSampleBank == existing.customSampleBank

    /**
     * Applies [sampleBank] and [sampleVolume] to a [HitSampleInfo] if necessary, returning the modified [HitSampleInfo].
     *
     * @param hitSampleInfo The [HitSampleInfo]. This will not be modified.
     * @return The modified [HitSampleInfo]. This does not share a reference with [hitSampleInfo].
     */
    fun applyTo(hitSampleInfo: HitSampleInfo) = hitSampleInfo.let { h ->
        val volume = h.volume.takeIf { it > 0 } ?: sampleVolume

        when (h) {
            is FileHitSampleInfo -> h.copy(volume = volume)

            is BankHitSampleInfo -> h.copy(
                volume = volume,
                bank = h.bank.takeIf { it != SampleBank.None } ?: sampleBank,
                customSampleBank = h.customSampleBank.takeIf { it > 0 } ?: customSampleBank
            )

            else -> throw IllegalArgumentException("Unknown type of hit sample.")
        }
    }
}