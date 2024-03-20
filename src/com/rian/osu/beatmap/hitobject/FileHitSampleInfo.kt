package com.rian.osu.beatmap.hitobject

/**
 * Represents a custom gameplay hit sample that can be loaded from files.
 */
class FileHitSampleInfo(
    /**
     * The name of the file to load the sample from.
     */
    @JvmField
    val filename: String,

    /**
     * The sample volume.
     *
     * If this is 0, the underlying control point's volume should be used instead.
     */
    volume: Int = 0
) : HitSampleInfo(volume) {
    fun copy(filename: String? = null, volume: Int? = null) = FileHitSampleInfo(filename ?: this.filename, volume ?: this.volume)
}