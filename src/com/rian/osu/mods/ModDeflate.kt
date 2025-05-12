package com.rian.osu.mods

import com.reco1l.toolkt.roundBy

/**
 * Represents the Deflate mod.
 */
class ModDeflate : ModObjectScaleTween() {
    override val name = "Deflate"
    override val acronym = "DF"
    override val description = "Hit them at the right size!"

    override var startScale by FloatModSetting(
        name = "Starting size",
        valueFormatter = { it.roundBy(1).toString() },
        defaultValue = 2f,
        minValue = 1f,
        maxValue = 5f,
        step = 0.1f,
        precision = 1
    )

    override fun deepCopy() = ModDeflate().also {
        it.startScale = startScale
    }
}