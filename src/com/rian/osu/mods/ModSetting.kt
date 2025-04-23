@file:Suppress("ktPropBy")

package com.rian.osu.mods

import com.rian.osu.math.preciseRoundBy
import kotlin.math.*
import kotlin.properties.*
import kotlin.reflect.*

//region Base classes

/**
 * Represents a [Mod] specific setting.
 */
sealed class ModSetting<V>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The value formatter of this [ModSetting].
     *
     * This is used to format the value of this [ModSetting] when displaying it.
     */
    val valueFormatter: ((V) -> String)?,

    /**
     * The default value of this [ModSetting], which is also the initial value of this [ModSetting].
     */
    var defaultValue: V

) : ReadWriteProperty<Any?, V> {

    /**
     * The initial value.
     */
    val initialValue = defaultValue

    /**
     * The value itself.
     */
    open var value = defaultValue


    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}

/**
 * Represents a [Mod] specific setting whose value is constrained to a range of values.
 */
sealed class RangeConstrainedModSetting<V>(
    name: String,
    valueFormatter: (V) -> String = { it.toString() },
    defaultValue: V,

    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    val minValue: V,

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    val maxValue: V,

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    val step: V,

) : ModSetting<V>(name, valueFormatter, defaultValue) {
    override var value
        get() = super.value
        set(value) {
            super.value = processValue(value)
        }

    protected abstract fun processValue(value: V): V

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        super.setValue(thisRef, property, processValue(value))
    }
}

//endregion


open class FloatModSetting(
    name: String,
    valueFormatter: (Float) -> String = { it.toString() },
    defaultValue: Float,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    val precision: Int? = null

) : RangeConstrainedModSetting<Float>(
    name,
    valueFormatter,
    if (precision != null) defaultValue.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step
) {
    init {
        require(this.minValue <= this.maxValue) { "minValue must be less than or equal to maxValue." }
        require(this.step >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }

        require(this.defaultValue in this.minValue..this.maxValue) {
            "defaultValue must be between minValue and maxValue."
        }
    }

    override fun processValue(value: Float) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0f -> value

        else -> {
            val value = round((value - minValue) / step) * step + minValue

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }
}

open class NullableFloatModSetting(
    name: String,
    valueFormatter: (Float?) -> String = { it.toString() },
    defaultValue: Float?,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    val precision: Int? = null

) : RangeConstrainedModSetting<Float?>(
    name,
    valueFormatter,
    if (precision != null) defaultValue?.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
) {
    init {
        require(this.minValue!! <= this.maxValue!!) { "minValue must be less than or equal to maxValue." }
        require(this.step!! >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }

        require(this.defaultValue == null || this.defaultValue!! in this.minValue..this.maxValue) {
            "defaultValue must be between minValue and maxValue."
        }
    }

    override fun processValue(value: Float?) = when {
        value == null -> null
        value < minValue!! -> minValue
        value > maxValue!! -> maxValue
        step == 0f -> value

        else -> {
            val value = round((value - minValue) / step!!) * step + minValue

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }
}

open class BooleanModSetting(
    name: String,
    defaultValue: Boolean
) : ModSetting<Boolean>(name, null, defaultValue)
