package com.rian.osu.beatmap.hitobject

/**
 * Represents a gameplay hit sample that is meant to be played sequentially at specific times.
 */
class SequenceHitSampleInfo(
    /**
     * The [HitSampleInfo]s to play, paired with the time at which they should be played.
     */
    @JvmField
    val samples: List<Pair<Double, HitSampleInfo>>
) {
    /**
     * Whether this [SequenceHitSampleInfo] contains no [HitSampleInfo]s.
     */
    fun isEmpty() = samples.isEmpty()

    /**
     * Obtains the [Pair] of [HitSampleInfo] and its time at a given index.
     */
    operator fun get(index: Int) = samples[index]

    /**
     * Obtains the [HitSampleInfo] to play at a given time.
     *
     * @param time The time, in milliseconds.
     * @return The [HitSampleInfo] to play at the given time, or `null` if no [HitSampleInfo]s should be played.
     */
    fun sampleAt(time: Double): HitSampleInfo? {
        if (samples.isEmpty() || time < samples[0].first) {
            return null
        }

        val lastSample = samples[samples.size - 1]
        if (time >= lastSample.first) {
            return lastSample.second
        }

        var l = 0
        var r = samples.size - 2

        while (l <= r) {
            val pivot = l + (r - l shr 1)
            val sample = samples[pivot]

            when {
                sample.first < time -> l = pivot + 1
                sample.first > time -> r = pivot - 1
                else -> return sample.second
            }
        }

        // l will be the first sample with time > sample.time, but we want the one before it
        return samples[l - 1].second
    }
}