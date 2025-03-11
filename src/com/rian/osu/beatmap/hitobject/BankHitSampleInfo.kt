package com.rian.osu.beatmap.hitobject

import com.rian.osu.beatmap.constants.SampleBank

/**
 * Represents a pre-determined gameplay hit sample that can be loaded from banks.
 */
class BankHitSampleInfo(
    /**
     * The name of the sample.
     */
    @JvmField
    val name: String,

    /**
     * The [SampleBank] to load the sample from.
     */
    @JvmField
    val bank: SampleBank = SampleBank.None,

    /**
     * The index of this [BankHitSampleInfo], if it uses custom samples.
     *
     * If this is 0, the underlying control point's sample index should be used instead.
     */
    @JvmField
    val customSampleBank: Int = 0,

    /**
     * The [BankHitSampleInfo]'s volume.
     *
     * If this is 0, the underlying control point's volume should be used instead.
     */
    volume: Int = 0,

    /**
     * Whether this [BankHitSampleInfo] is layered.
     *
     * Layered hit samples are automatically added, but can be disabled using the layered skin config option.
     */
    @JvmField
    val isLayered: Boolean = false
) : HitSampleInfo(volume) {
    override val lookupNames = mutableListOf<String>().also {
        if (customSampleBank >= 2) {
            it.add("${bank.prefix}-${name}${customSampleBank}")
        }

        it.add("${bank.prefix}-${name}")
        it.add(name)
    }

    fun copy(name: String? = null, bank: SampleBank? = null, customSampleBank: Int? = null, volume: Int? = null, isLayered: Boolean? = null) =
        BankHitSampleInfo(name ?: this.name, bank ?: this.bank,
            customSampleBank ?: this.customSampleBank, volume ?: this.volume, isLayered ?: this.isLayered)

    companion object {
        const val HIT_WHISTLE = "hitwhistle"
        const val HIT_FINISH = "hitfinish"
        const val HIT_NORMAL = "hitnormal"
        const val HIT_CLAP = "hitclap"
    }
}