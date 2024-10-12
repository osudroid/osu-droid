package com.rian.osu.beatmap.hitobject

import ru.nsu.ccfit.zuev.osu.game.GameObjectListener

/**
 * A [SequenceHitSampleInfo] that can directly be used in gameplay to play samples.
 */
class GameplaySequenceHitSampleInfo {
    private var listener: GameObjectListener? = null
    private var sampleInfo: SequenceHitSampleInfo? = null
    private var index = 0
    private var isPlaying = false
    private var elapsedTime = 0f
    private var looped = false

    /**
     * Whether this [GameplaySequenceHitSampleInfo] has been initialized.
     */
    val isInitialized
        get() = listener != null && sampleInfo != null

    private val canPlay
        get() = isInitialized && !sampleInfo!!.isEmpty()

    /**
     * Initializes this [GameplaySequenceHitSampleInfo].
     *
     * @param listener The [GameObjectListener] to use for playing the samples.
     * @param startTime The starting time point of this [GameplaySequenceHitSampleInfo].
     * @param sampleInfo The [SequenceHitSampleInfo] to use this [GameplaySequenceHitSampleInfo] with.
     */
    fun init(listener: GameObjectListener, startTime: Float, sampleInfo: SequenceHitSampleInfo) {
        reset()

        this.listener = listener
        this.elapsedTime = startTime
        this.sampleInfo = sampleInfo
    }

    /**
     * Plays this [GameplaySequenceHitSampleInfo].
     *
     * @param looped Whether to loop the samples. Defaults to `false`.
     */
    @JvmOverloads
    fun play(looped: Boolean = false) {
        this.looped = looped

        // Prevent playing the sample if it's already playing or if it can't be played.
        if (!canPlay || (looped && isPlaying)) {
            return
        }

        listener!!.playSample(sampleInfo!![index].second, looped)
        isPlaying = true
    }

    /**
     * Stops this [GameplaySequenceHitSampleInfo]'s playback.
     */
    fun stop() {
        if (!canPlay || !isPlaying) {
            return
        }

        listener!!.stopSample(sampleInfo!![index].second)
        isPlaying = false
    }

    /**
     * Updates the state of this [GameplaySequenceHitSampleInfo].
     *
     * @param deltaTime The time that has passed since the last update, in milliseconds.
     */
    fun update(deltaTime: Float) {
        elapsedTime += deltaTime

        if (!canPlay) {
            return
        }

        if (elapsedTime < sampleInfo!![0].first) {
            return
        }

        while (index < sampleInfo!!.samples.size - 1 && sampleInfo!![index + 1].first > elapsedTime) {
            if (looped && isPlaying) {
                // Stop the currently looping sample and play the correct one later.
                stop()
            }

            ++index
        }

        if (looped && !isPlaying) {
            // Play the correct looped sample at this time point.
            play(true)
        }
    }

    /**
     * Resets this [GameplaySequenceHitSampleInfo] to its initial state.
     */
    fun reset() {
        listener = null
        sampleInfo = null
        index = 0
        elapsedTime = 0f
        looped = false
        isPlaying = false
    }
}