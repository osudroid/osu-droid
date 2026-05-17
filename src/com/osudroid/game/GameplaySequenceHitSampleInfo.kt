package com.osudroid.game

import com.reco1l.toolkt.kotlin.fastForEach
import com.osudroid.beatmaps.hitobjects.HitSampleInfo
import com.osudroid.beatmaps.hitobjects.SequenceHitSampleInfo

/**
 * A wrapper for [SequenceHitSampleInfo]s to automatically configure [HitSampleInfo]s to play at certain times
 * in gameplay.
 */
class GameplaySequenceHitSampleInfo : IGameplayHitSampleInfo {
    override var frequency = 1f
        set(value) {
            field = value

            samples.fastForEach { it.frequency = value }
        }

    override var isLooping = false
        set(value) {
            field = value

            samples.fastForEach { it.isLooping = value }
        }

    override var volume = 1f
        set(value) {
            field = value

            samples.fastForEach { it.volume = value }
        }

    private var index = 0
    private var samples = ArrayList<GameplayHitSampleInfo>()
    private var elapsedTime = 0f
    private var hasSamplePlaying = false

    private val currentSampleInfo
        get() = samples.getOrNull(index)

    override val isInitialized
        get() = samples.isNotEmpty()

    private val sampleNeedsUpdating
        get() = isInitialized && index < samples.size - 1 && elapsedTime >= samples[index + 1].time

    /**
     * Initializes this [GameplaySequenceHitSampleInfo].
     *
     * @param startTime The starting time point of this [GameplaySequenceHitSampleInfo].
     * @param sampleInfo The [SequenceHitSampleInfo] to use this [GameplaySequenceHitSampleInfo] with.
     */
    fun init(startTime: Float, sampleInfo: SequenceHitSampleInfo) {
        reset()

        elapsedTime = startTime

        samples.ensureCapacity(sampleInfo.samples.size)

        sampleInfo.samples.fastForEach { (time, sample) ->
            val gameplaySampleInfo = GameplayHitSampleInfo.obtain().also {
                it.init(sample)
                it.time = time
                it.frequency = frequency
                it.isLooping = isLooping
            }

            samples.add(gameplaySampleInfo)
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

        samples.fastForEach { it.stop() }
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

        samples.fastForEach { it.release() }
        samples.clear()
        index = 0
        elapsedTime = 0f
        frequency = 1f
        volume = 1f
        isLooping = false
        hasSamplePlaying = false
    }
}