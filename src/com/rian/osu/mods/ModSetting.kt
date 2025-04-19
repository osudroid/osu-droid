@file:Suppress("ktPropBy")

package com.rian.osu.mods

import com.rian.osu.math.preciseRoundBy
import kotlin.math.*
import kotlin.properties.*
import kotlin.reflect.*

//region Base classes

private val DEFAULT_FORMATTER: (Any?) -> String = { it.toString() }

/**
 * Represents a [Mod] specific setting.
 */
sealed class ModSetting<T>(

    /**
     * The legible name of this [ModSetting].
     */
    val name: String,

    /**
     * The value formatter of this [ModSetting].
     *
     * This is used to format the value of this [ModSetting] when displaying it.
     */
    val valueFormatter: ((T) -> String)?,

    /**
     * The default value of this [ModSetting], which is also the initial value of this [ModSetting].
     */
    val defaultValue: T

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
 * Represents a [Mod] specific setting whose value is constrained to a range of values.
 */
sealed class RangeConstrainedModSetting<T>(
    name: String,
    valueFormatter: (T) -> String = DEFAULT_FORMATTER,
    defaultValue: T,

    /**
     * The minimum value of this [RangeConstrainedModSetting].
     */
    val minValue: T,

    /**
     * The maximum value of this [RangeConstrainedModSetting].
     */
    val maxValue: T,

    /**
     * The step size for the value of this [RangeConstrainedModSetting].
     */
    val step: T,

) : ModSetting<T>(name, valueFormatter, defaultValue) {

    protected abstract fun processValue(value: T): T

    final override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        super.setValue(thisRef, property, processValue(value))
    }
}

//endregion


class FloatModSetting(
    name: String,
    valueFormatter: (Float) -> String = DEFAULT_FORMATTER,
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

class NullableFloatModSetting(
    name: String,
    valueFormatter: (Float?) -> String = DEFAULT_FORMATTER,
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

class BooleanModSetting(
    name: String,
    defaultValue: Boolean
) : ModSetting<Boolean>(name, null, defaultValue)
