package com.rian.osu.mods.settings

import com.rian.osu.math.*
import kotlin.math.*
import kotlinx.serialization.json.*

/**
 * A [ModSetting] that represents a [Float] value with range constraints.
 */
open class FloatModSetting(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<Float>.(Float) -> String = { it.toString() },
    defaultValue: Float,
    minValue: Float = 0f,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,
    precision: Int? = null,
    orderPosition: Int? = null,
    useManualInput: Boolean = false
) : NumberModSetting<Float>(
    name,
    key,
    valueFormatter,
    if (precision != null) defaultValue.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
    orderPosition,
    useManualInput
) {
    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    var precision = precision
        set(value) {
            if (field != value) {
                require(value == null || value >= 0) { "precision must be greater than or equal to 0." }

                field = value

                if (value != null) {
                    // Trigger processValue to ensure the value is within the new range
                    this.value = this.value
                }
            }
        }

    init {
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }
    }

    override fun load(json: JsonObject) {
        if (key != null) {
            value = json[key]?.jsonPrimitive?.floatOrNull ?: defaultValue
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key != null) {
            builder.put(key, value)
        }
    }

    override fun processValue(value: Float): Float {
        val precision = precision
        var processedValue = super.processValue(value)

        if (precision != null) {
           processedValue = processedValue.preciseRoundBy(precision)
        }

        return processedValue
    }

    override fun snapToStep(value: Float) = round((value - minValue) / step) * step + minValue

    override fun copyFrom(other: ModSetting<Float>) {
        if (other is FloatModSetting) {
            precision = other.precision
        }

        super.copyFrom(other)
    }
}

/**
 * A [ModSetting] that represents a nullable [Float] value with range constraints.
 */
open class NullableFloatModSetting(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<Float?>.(Float?) -> String = { it.toString() },
    defaultValue: Float?,
    minValue: Float = 0f,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,
    precision: Int? = null,
    orderPosition: Int? = null,
    useManualInput: Boolean = false
) : NullableNumberModSetting<Float>(
    name,
    key,
    valueFormatter,
    if (precision != null) defaultValue?.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
    orderPosition,
    useManualInput
) {
    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    var precision = precision
        set(value) {
            if (value != null && value < 0) {
                throw IllegalArgumentException("precision must be greater than or equal to 0.")
            }

            field = value

            if (value != null) {
                // Trigger processValue to ensure the value is within the new range
                this.value = this.value
            }
        }

    init {
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }
    }

    override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]

        value =
            if (element is JsonNull) null
            else element?.jsonPrimitive?.floatOrNull ?: defaultValue
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key == null) {
            return
        }

        if (value == null) {
            builder.put(key, JsonNull)
            return
        }

        builder.put(key, value)
    }

    override fun processValue(value: Float?): Float? {
        val precision = precision
        var processedValue = super.processValue(value)

        if (precision != null && processedValue != null) {
           processedValue = processedValue.preciseRoundBy(precision)
        }

        return processedValue
    }

    override fun snapToStep(value: Float) = round((value - minValue) / step) * step + minValue

    override fun copyFrom(other: ModSetting<Float?>) {
        if (other is NullableFloatModSetting) {
            precision = other.precision
        }

        super.copyFrom(other)
    }
}