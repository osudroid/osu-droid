package com.rian.osu.gameplay

import com.reco1l.framework.Pool
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo
import com.rian.osu.beatmap.hitobject.HitSampleInfo
import ru.nsu.ccfit.zuev.audio.BassSoundProvider
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * A wrapper for [HitSampleInfo]s to allow for additional gameplay-specific information to be stored.
 */
class GameplayHitSampleInfo : IGameplayHitSampleInfo {
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

    private var sampleInfo: HitSampleInfo? = null
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
            soundProvider = getResources().getCustomSound(sampleInfo.lookupNames[i], false)

            if (soundProvider != null) {
                soundProvider!!.setFrequency(frequency)
                soundProvider!!.setLooping(isLooping)
                break
            }
        }
    }

    override fun play() {
        if (!OsuSkin.get().isLayeredHitSounds && (sampleInfo as? BankHitSampleInfo)?.isLayered == true) {
            return
        }

        soundProvider?.play(sampleInfo!!.volume / 100f)
    }

    override fun stop() {
        soundProvider?.stop()
    }

    override fun reset() {
        frequency = 1f
        isLooping = false
        sampleInfo = null
        soundProvider = null
    }

    companion object {
        /**
         * A [Pool] of [GameplayHitSampleInfo]s.
         */
        @JvmField
        val pool = Pool(25) { GameplayHitSampleInfo() }
    }
}