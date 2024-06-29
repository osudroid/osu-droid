package com.rian.osu.beatmap.hitobject

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
    fun copy(volume: Int? = null) = HitSampleInfo(volume ?: this.volume)
}