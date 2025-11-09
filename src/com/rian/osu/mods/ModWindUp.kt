package com.rian.osu.mods

import com.reco1l.toolkt.roundBy
import com.rian.osu.mods.settings.*
import kotlin.reflect.KProperty

/**
 * Represents the Wind Up mod.
 */
class ModWindUp : ModTimeRamp() {
    override val name = "Wind Up"
    override val acronym = "WU"
    override val description = "Can you keep up?"
    override val type = ModType.Fun

    override var initialRate by object : FloatModSetting(
        name = "Initial rate",
        valueFormatter = { "${it.roundBy(2)}x" },
        defaultValue = 1f,
        minValue = 0.5f,
        maxValue = 1.95f,
        step = 0.05f,
        precision = 2,
        orderPosition = 0
    ) {
        override var value
            get() = super.value
            set(value) {
                super.value = value
                onInitialRateChange()
            }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            super.setValue(thisRef, property, value)
            onInitialRateChange()
        }
    }

    override var finalRate by object : FloatModSetting(
        name = "Final rate",
        valueFormatter = { "${it.roundBy(2)}x" },
        defaultValue = 1.5f,
        minValue = 0.55f,
        maxValue = 2f,
        step = 0.05f,
        precision = 2,
        orderPosition = 1
    ) {
        override var value
            get() = super.value
            set(value) {
                super.value = value
                onFinalRateChange()
            }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
            super.setValue(thisRef, property, value)
            onFinalRateChange()
        }
    }

    private fun onInitialRateChange() {
        if (initialRate >= finalRate) {
            finalRate = initialRate + getModSettingDelegate<FloatModSetting>(::finalRate).step
        }
    }

    private fun onFinalRateChange() {
        if (finalRate <= initialRate) {
            initialRate = finalRate - getModSettingDelegate<FloatModSetting>(::initialRate).step
        }
    }
}