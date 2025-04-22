package com.rian.osu.mods

import com.reco1l.toolkt.roundBy
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

class ModWindDown : ModTimeRamp() {
    override val name = "Wind Down"
    override val acronym = "WD"
    override val description = "Sloooow doooown..."
    override val type = ModType.Fun
    override val textureNameSuffix = "winddown"

    override var initialRate by object : FloatModSetting(
        name = "Initial rate",
        valueFormatter = { "${it.roundBy(2)}x" },
        defaultValue = 1f,
        minValue = 0.55f,
        maxValue = 2f,
        step = 0.05f,
        precision = 2
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
        defaultValue = 0.75f,
        minValue = 0.5f,
        maxValue = 1.95f,
        step = 0.05f,
        precision = 2
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
        if (initialRate <= finalRate) {
            finalRate = initialRate - (::finalRate).run {
                isAccessible = true
                (getDelegate() as FloatModSetting).step
            }
        }
    }

    private fun onFinalRateChange() {
        if (finalRate >= initialRate) {
            initialRate = finalRate + (::initialRate).run {
                isAccessible = true
                (getDelegate() as FloatModSetting).step
            }
        }
    }

    override fun deepCopy() = ModWindDown().also {
        it.initialRate = initialRate
        it.finalRate = finalRate
    }
}