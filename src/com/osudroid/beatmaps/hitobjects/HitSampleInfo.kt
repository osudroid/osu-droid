package com.osudroid.beatmaps.hitobjects

/**
 * Represents a gameplay hit sample.
 */
open class HitSampleInfo protected constructor(
    /**
     * The sample volume.
     *
     * If this is 0, the underlying control point's volume should be used instead.
     */
    @JvmField
    val volume: Int = 0
) {
    /**
     * All possible filenames that can be used as an audio source, returned in order of preference (highest first).
     */
    open val lookupNames = emptyList<String>()

    /**
     * Whether this [HitSampleInfo] can be resolved from the beatmap's own custom samples.
     */
    open val useBeatmapSample = true

    fun copy(volume: Int? = null) = HitSampleInfo(volume ?: this.volume)
}