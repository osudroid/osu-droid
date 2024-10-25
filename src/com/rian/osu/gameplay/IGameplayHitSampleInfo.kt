package com.rian.osu.gameplay

import com.rian.osu.beatmap.hitobject.HitSampleInfo

/**
 * An interface for wrapped [HitSampleInfo] for gameplay purposes.
 */
interface IGameplayHitSampleInfo {
    /**
     * Whether this [IGameplayHitSampleInfo] has been initialized.
     */
    val isInitialized: Boolean

    /**
     * Sets the rate at which this [IGameplayHitSampleInfo] is played back (affects pitch).
     * 1 is 100% playback speed, or default frequency.
     */
    var frequency: Float

    /**
     * Whether this [IGameplayHitSampleInfo] should loop its playback.
     */
    var isLooping: Boolean

    /**
     * Plays this [IGameplayHitSampleInfo].
     */
    fun play()

    /**
     * Stops this [IGameplayHitSampleInfo].
     */
    fun stop()

    /**
     * Resets this [IGameplayHitSampleInfo] to its initial state.
     */
    fun reset()
}