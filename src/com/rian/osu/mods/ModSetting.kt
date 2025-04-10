@file:Suppress("ktPropBy")

package com.rian.osu.mods

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
    var value = defaultValue


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

    protected abstract fun processValue(value: V): V

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        super.setValue(thisRef, property, processValue(value))
    }
}

//endregion


class FloatModSetting(
    name: String,
    valueFormatter: (Float) -> String = { it.toString() },
    defaultValue: Float,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f
) : RangeConstrainedModSetting<Float>(name, valueFormatter, defaultValue, minValue, maxValue, step) {
    override fun processValue(value: Float) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0f -> value
        else -> ceil((value - minValue) / step) * step + minValue
    }
}

class NullableFloatModSetting(
    name: String,
    valueFormatter: (Float?) -> String = { it.toString() },
    defaultValue: Float?,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f
) : RangeConstrainedModSetting<Float?>(name, valueFormatter, defaultValue, minValue, maxValue, step) {
    override fun processValue(value: Float?) = when {
        value == null -> null
        value < minValue!! -> minValue
        value > maxValue!! -> maxValue
        step == 0f -> value
        else -> ceil((value - minValue) / step!!) * step + minValue
    }
}

class BooleanModSetting(
    name: String,
    defaultValue: Boolean
) : ModSetting<Boolean>(name, null, defaultValue)
