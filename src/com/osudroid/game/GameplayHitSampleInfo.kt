package com.osudroid.game

import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.beatmaps.hitobjects.HitSampleInfo
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import kotlin.math.max
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * A wrapper for [HitSampleInfo]s to allow for additional gameplay-specific information to be stored.
 */
class GameplayHitSampleInfo : IGameplayHitSampleInfo, IPoolable {
    override var isRecycled = false

    /**
     * The time at which this [GameplayHitSampleInfo] should be played, in seconds.
     *
     * Used when this [GameplayHitSampleInfo] is played in sequence (see [GameplaySequenceHitSampleInfo]).
     */
    var time = 0.0

    override var frequency = 1f
        set(value) {
            field = value

            soundProvider?.setFrequency(value)
        }

    override var isLooping = false
        set(value) {
            field = value

            soundProvider?.setLooping(value)
        }

    override var volume = 1f
        set(value) {
            field = value

            soundProvider?.setVolume(getFinalVolume(value))
        }

    /**
     * The [HitSampleInfo] this [GameplayHitSampleInfo] wraps.
     */
    var sampleInfo: HitSampleInfo? = null
        private set

    private var soundProvider: BassSoundProvider? = null

    override val isInitialized
        get() = sampleInfo != null

    /**
     * Initializes this [GameplayHitSampleInfo].
     *
     * @param sampleInfo The [HitSampleInfo] to use this [GameplayHitSampleInfo] with.
     */
    fun init(sampleInfo: HitSampleInfo) {
        reset()

        this.sampleInfo = sampleInfo

        for (i in sampleInfo.lookupNames.indices) {
            val name = sampleInfo.lookupNames[i]
            val soundProvider =
                if (sampleInfo.useBeatmapSample) getResources().getCustomSound(name, false)
                else getResources().getSound(name, false)

            if (soundProvider != null) {
                soundProvider.setFrequency(frequency)
                soundProvider.setLooping(isLooping)
                this.soundProvider = soundProvider
                break
            }
        }
    }

    override fun play() {
        if (!OsuSkin.get().isLayeredHitSounds && (sampleInfo as? BankHitSampleInfo)?.isLayered == true) {
            return
        }

        soundProvider?.play(getFinalVolume(volume))
    }

    override fun stop() {
        soundProvider?.stop()
    }

    override fun reset() {
        time = 0.0
        frequency = 1f
        volume = 1f
        isLooping = false
        sampleInfo = null
        soundProvider = null
    }

    /**
     * Releases this [GameplayHitSampleInfo] back to the pool.
     */
    fun release() {
        reset()

        pool.release(this)
    }

    private fun getFinalVolume(volume: Float) = volume * max(0.05f, sampleInfo!!.volume / 100f)

    companion object {
        private val pool = SynchronizedPool<GameplayHitSampleInfo>(25).apply { release(GameplayHitSampleInfo()) }

        /**
         * Obtains a [GameplayHitSampleInfo] from the pool or creates a new one if the pool is empty.
         */
        @JvmStatic
        fun obtain() = pool.acquire() ?: GameplayHitSampleInfo()
    }
}