package com.rian.osu.gameplay

import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.hitobject.HitSampleInfo
import com.rian.osu.beatmap.hitobject.SequenceHitSampleInfo

/**
 * A wrapper for [SequenceHitSampleInfo]s to automatically configure [HitSampleInfo]s to play at certain times
 * in gameplay.
 */
class GameplaySequenceHitSampleInfo : IGameplayHitSampleInfo {
    override var frequency = 1f
        set(value) {
            field = value

            samples?.fastForEach { it.second.frequency = value }
        }

    override var isLooping = false
        set(value) {
            field = value

            samples?.fastForEach { it.second.isLooping = value }
        }

    private var index = 0
    private var samples: Array<Pair<Double, GameplayHitSampleInfo>>? = null
    private var elapsedTime = 0f
    private var hasSamplePlaying = false

    private val currentSampleInfo
        get() = samples?.get(index)?.second

    override val isInitialized
        get() = samples != null

    private val sampleNeedsUpdating
        get() = isInitialized && index < samples!!.size - 1 && elapsedTime >= samples!![index + 1].first

    /**
     * Initializes this [GameplaySequenceHitSampleInfo].
     *
     * @param startTime The starting time point of this [GameplaySequenceHitSampleInfo].
     * @param sampleInfo The [SequenceHitSampleInfo] to use this [GameplaySequenceHitSampleInfo] with.
     */
    fun init(startTime: Float, sampleInfo: SequenceHitSampleInfo) {
        reset()

        elapsedTime = startTime

        samples = Array(sampleInfo.samples.size) { i ->
            val sample = sampleInfo.samples[i]

            val gameplaySampleInfo = GameplayHitSampleInfo.pool.obtain().also {
                it.init(sample.second)
                it.frequency = frequency
                it.isLooping = isLooping
            }

            sample.first to gameplaySampleInfo
        }
    }

    override fun play() {
        // Do not allow playing loop samples if a sample is already playing.
        if (isLooping && hasSamplePlaying) {
            return
        }

        hasSamplePlaying = true
        currentSampleInfo?.play()
    }

    override fun stop() {
        hasSamplePlaying = false
        currentSampleInfo?.stop()
    }

    /**
     * Forcefully stops all [GameplayHitSampleInfo]s in this [GameplaySequenceHitSampleInfo].
     */
    fun stopAll() {
        hasSamplePlaying = false

        samples?.fastForEach { it.second.stop() }
    }

    /**
     * Updates the state of this [GameplaySequenceHitSampleInfo].
     *
     * @param deltaTime The time that has passed since the last update, in milliseconds.
     */
    fun update(deltaTime: Float) {
        elapsedTime += deltaTime

        if (!sampleNeedsUpdating) {
            return
        }

        val hadSamplePlaying = hasSamplePlaying

        // Stop the currently looping sample if its lifetime has ended.
        if (isLooping && hasSamplePlaying) {
            stop()
        }

        while (sampleNeedsUpdating) {
            ++index
        }

        // Play the new looping sample.
        if (isLooping && hadSamplePlaying) {
            play()
        }
    }

    override fun reset() {
        // Ensure all samples are stopped before resetting.
        if (isLooping) {
            stopAll()
        }

        samples?.fastForEach {
            it.second.reset()
            GameplayHitSampleInfo.pool.free(it.second)
        }

        samples = null
        index = 0
        elapsedTime = 0f
        frequency = 1f
        isLooping = false
        hasSamplePlaying = false
    }
}