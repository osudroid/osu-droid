@file:Suppress("ktPropBy")

package com.rian.osu.mods

import kotlin.math.*
import kotlin.properties.*
import kotlin.reflect.*

//region Base classes

private val DEFAULT_FORMATTER: (Any?) -> String = { it.toString() }

/**
 * Represents a mod specific setting.
 */
sealed class ModSetting<T>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The value formatter, this is used to format the value when displaying it.
     */
    val valueFormatter: ((T) -> String)?,

    /**
     * The default value, this will be also the initial value.
     */
    var defaultValue: T

) : ReadWriteProperty<Any?, T> {


    /**
     * The value itself.
     */
    var value = defaultValue


    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Represents a mod specific setting that is restricted to a range of values.
 */
sealed class RestrictedModSetting<T>(
    name: String,
    valueFormatter: (T) -> String = DEFAULT_FORMATTER,
    defaultValue: T,

    /**
     * The minimum value.
     */
    val min: T,

    /**
     * The maximum value.
     */
    val max: T,

    /**
     * Defines the step size for the value. Default is 0 which means no step.
     */
    val step: T,

) : ModSetting<T>(name, valueFormatter, defaultValue) {

    abstract fun processValue(value: T): T

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        super.setValue(thisRef, property, processValue(value))
    }
}

//endregion


class FloatModSetting(
    name: String,
    valueFormatter: (Float) -> String = DEFAULT_FORMATTER,
    defaultValue: Float,
    min: Float = Float.MIN_VALUE,
    max: Float = Float.MAX_VALUE,
    step: Float = 0f
) : RestrictedModSetting<Float>(name, valueFormatter, defaultValue, min, max, step) {
    override fun processValue(value: Float) = when {
        value < min -> min
        value > max -> max
        step == 0f -> value
        else -> ceil((value - min) / step) * step + min
    }
}

class NullableFloatModSetting(
    name: String,
    valueFormatter: (Float?) -> String = DEFAULT_FORMATTER,
    defaultValue: Float?,
    min: Float = Float.MIN_VALUE,
    max: Float = Float.MAX_VALUE,
    step: Float = 0f
) : RestrictedModSetting<Float?>(name, valueFormatter, defaultValue, min, max, step) {
    override fun processValue(value: Float?) = when {
        value == null -> null
        value < min!! -> min
        value > max!! -> max
        step == 0f -> value
        else -> ceil((value - min) / step!!) * step + min
    }
}

class BooleanModSetting(
    name: String,
    defaultValue: Boolean
) : ModSetting<Boolean>(name, null, defaultValue)



