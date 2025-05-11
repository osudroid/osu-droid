package com.rian.osu.mods

import com.reco1l.toolkt.roundBy

/**
 * Represents the Grow mod.
 */
class ModGrow : ModObjectScaleTween() {
    override val name = "Grow"
    override val acronym = "GR"
    override val description = "Hit them at the right size!"

    override var startScale by FloatModSetting(
        name = "Starting size",
        valueFormatter = { it.roundBy(2).toString() },
        defaultValue = 0.5f,
        minValue = 0f,
        maxValue = 0.99f,
        step = 0.01f,
        precision = 2
    )

    override fun deepCopy() = ModGrow().also {
        it.startScale = startScale
    }
}