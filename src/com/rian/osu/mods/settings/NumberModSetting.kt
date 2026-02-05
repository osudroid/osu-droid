package com.rian.osu.mods.settings

import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.serialization.json.*

/**
 * An interface for [ModSetting]s that represent numeric values with step increments.
 */
interface INumberModSetting<T> where T : Number, T : Comparable<T> {
    /**
     * The step increment for this [INumberModSetting].
     *
     * If set to 0, no stepping is applied.
     */
    var step: T
}

/**
 * A [ModSetting] that represents a numeric value with range constraints and step increments.
 */
open class NumberModSetting<T>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,
    minValue: T,
    maxValue: T,
    step: T,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NumberModSetting] manually.
     */
    @get:JvmName("isUseManualInput")
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<T>(
    name,
    key,
    valueFormatter,
    defaultValue,
    minValue,
    maxValue,
    orderPosition
), INumberModSetting<T> where T : Number, T : Comparable<T> {
    final override var step = step
        set(value) {
            if (field != value) {
                if (value.toDouble() < 0) {
                    throw IllegalArgumentException("step must be non-negative.")
                }

                field = value

                // Re-apply the current value to enforce the new step.
                this.value = this.value
            }
        }

    init {
        require(step.toDouble() >= 0) { "step must be non-negative." }
    }

    override fun processValue(value: T): T {
        var processedValue = super.processValue(value)

        if (step.toDouble() > 0) {
            processedValue = snapToStep(processedValue, minValue, step)
        }

        return processedValue
    }

    override fun copyFrom(other: ModSetting<T>) {
        if (other is NumberModSetting<T>) {
            step = other.step
        }

        super.copyFrom(other)
    }

    final override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]?.jsonPrimitive

        @Suppress("UNCHECKED_CAST")
        value = when (value) {
            is Int -> element?.intOrNull
            is Long -> element?.longOrNull
            is Float -> element?.floatOrNull
            is Double -> element?.doubleOrNull
            else -> null
        } as? T ?: defaultValue
    }

    final override fun save(builder: JsonObjectBuilder) = saveToJSON(builder)
}

/**
 * A [ModSetting] that represents a nullable numeric value with range constraints and step increments.
 */
open class NullableNumberModSetting<T>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T?>.(T?) -> String = { it.toString() },
    defaultValue: T?,
    minValue: T,
    maxValue: T,
    step: T,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NullableNumberModSetting] manually.
     */
    val useManualInput: Boolean = false

) : NullableRangeConstrainedModSetting<T>(
    name,
    key,
    valueFormatter,
    defaultValue,
    minValue,
    maxValue,
    orderPosition
), INumberModSetting<T> where T : Number, T : Comparable<T> {
    final override var step = step
        set(value) {
            if (field != value) {
                if (value.toDouble() < 0) {
                    throw IllegalArgumentException("step must be non-negative.")
                }

                field = value

                // Re-apply the current value to enforce the new step.
                this.value = this.value
            }
        }

    init {
        require(step.toDouble() >= 0) { "step must be non-negative." }
    }

    override fun processValue(value: T?): T? {
        var processedValue = super.processValue(value)

        if (processedValue != null && step.toDouble() > 0) {
            processedValue = snapToStep(processedValue, minValue, step)
        }

        return processedValue
    }

    override fun copyFrom(other: ModSetting<T?>) {
        if (other is NullableNumberModSetting<T>) {
            step = other.step
        }

        super.copyFrom(other)
    }

    final override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]

        if (element is JsonNull) {
            value = null
            return
        }

        val primitive = element?.jsonPrimitive

        // Take advantage of minValue's non-nullability to determine the type of T
        @Suppress("UNCHECKED_CAST")
        value = when (minValue) {
            is Int -> primitive?.intOrNull
            is Long -> primitive?.longOrNull
            is Float -> primitive?.floatOrNull
            is Double -> primitive?.doubleOrNull
            else -> null
        } as? T ?: defaultValue
    }

    final override fun save(builder: JsonObjectBuilder) = saveToJSON(builder)
}

/**
 * Saves a [Number] or nullable [Number] value in a [ModSetting] to a [JsonObjectBuilder].
 *
 * @param builder The [JsonObjectBuilder] to save the value to.
 */
private fun <T : Number?> ModSetting<T>.saveToJSON(builder: JsonObjectBuilder) {
    if (key == null) {
        return
    }

    if (value == null) {
        builder.put(key, JsonNull)
        return
    }

    builder.put(key, value)
}

/**
 * Snaps a [Number] value to the nearest step increment.
 *
 * @param value The value to snap.
 * @param minValue The minimum value of the range.
 * @param step The step increment.
 * @return The snapped value.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Number> snapToStep(value: T, minValue: T, step: T) =
    when {
        value is Int && minValue is Int && step is Int ->
            (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()

        value is Long && minValue is Long && step is Long ->
            (round((value - minValue) / step.toDouble()) * step + minValue).roundToLong()

        value is Float && minValue is Float && step is Float ->
            round((value - minValue) / step) * step + minValue

        value is Double && minValue is Double && step is Double ->
            round((value - minValue) / step) * step + minValue

        else -> value
    } as T
