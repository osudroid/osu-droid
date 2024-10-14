package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.constants.BeatmapCountdown
import com.rian.osu.beatmap.constants.SampleBank

/**
 * Contains general information about a beatmap.
 */
data class BeatmapGeneral(
    /**
     * The location of the audio file relative to the beatmapset file.
     */
    @JvmField
    var audioFilename: String = "",

    /**
     * The amount of milliseconds of silence before the audio starts playing.
     */
    @JvmField
    var audioLeadIn: Int = 0,

    /**
     * The time in milliseconds when the audio preview should start.
     *
     * If -1, the audio should begin playing at 40% of its length.
     */
    @JvmField
    var previewTime: Int = -1,

    /**
     * The speed of the countdown before the first hit object.
     */
    @JvmField
    var countdown: BeatmapCountdown = BeatmapCountdown.Normal,

    /**
     * The sample bank that will be used if timing points do not override it.
     */
    @JvmField
    var sampleBank: SampleBank = SampleBank.Normal,

    /**
     * The sample volume that will be used if timing points do not override it.
     */
    @JvmField
    var sampleVolume: Int = 100,

    /**
     * The multiplier for the threshold in time where hit objects
     * placed close together stack, ranging from 0 to 1.
     */
    @JvmField
    var stackLeniency: Float = 0.7f,

    /**
     * Whether breaks have a letterboxing effect.
     */
    @JvmField
    var letterboxInBreaks: Boolean = false,

    /**
     * The game mode this beatmap represents.
     */
    @JvmField
    var mode: Int = 0,

    /**
     * Whether sound samples will change rate when playing with rate-adjusting mods.
     */
    @JvmField
    var samplesMatchPlaybackRate: Boolean = false
)