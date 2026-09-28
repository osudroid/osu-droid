package com.osudroid.storyboard.model

/**
 * Represents a storyboard `Sample` (audio) event.
 */
class StoryboardSample(
    /**
     * The layer this sample belongs to.
     */
    @JvmField
    val layer: StoryboardLayerType,

    /**
     * The time this sample is played at, in milliseconds.
     */
    @JvmField
    val time: Double,

    /**
     * The path of the audio file, relative to the beatmap folder.
     */
    @JvmField
    val filePath: String,

    /**
     * The volume of the sample in the range `[0, 100]`.
     */
    @JvmField
    val volume: Int
)
