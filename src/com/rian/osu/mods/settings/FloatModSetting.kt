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
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    precision: Int? = null,

    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [FloatModSetting] manually.
     */
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<Float>(
    name,
    key,
    valueFormatter,
    if (precision != null) defaultValue.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
    orderPosition
) {
    override var defaultValue
        get() = super.defaultValue
        set(value) {
            if (value !in minValue..maxValue) {
                throw IllegalArgumentException("defaultValue must be between minValue and maxValue.")
            }

            super.defaultValue = value
        }

    override var minValue
        get() = super.minValue
        set(value) {
            super.minValue = min(value, maxValue)
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            super.maxValue = max(value, minValue)
        }

    override var step
        get() = super.step
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

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
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }
        require(defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
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

    override fun processValue(value: Float) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0f -> value

        else -> {
            val value = round((value - minValue) / step) * step + minValue
            val precision = precision

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }

    override fun copyFrom(other: ModSetting<Float>) {
        super.copyFrom(other)

        if (other is FloatModSetting) {
            precision = other.precision
        }
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
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    step: Float = 0f,

    /**
     * The number of decimal places to round the value to.
     *
     * When set to `null`, the value will not be rounded.
     */
    precision: Int? = null,

    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NullableFloatModSetting] manually.
     */
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<Float?>(
    name,
    key,
    valueFormatter,
    if (precision != null) defaultValue?.preciseRoundBy(precision) else defaultValue,
    if (precision != null) minValue.preciseRoundBy(precision) else minValue,
    if (precision != null) maxValue.preciseRoundBy(precision) else maxValue,
    if (precision != null) step.preciseRoundBy(precision) else step,
    orderPosition
) {
    override var defaultValue
        get() = super.defaultValue
        set(value) {
            if (value != null && value !in minValue..maxValue) {
                throw IllegalArgumentException("defaultValue must be between minValue and maxValue.")
            }

            super.defaultValue = value
        }

    override var minValue
        get() = super.minValue
        set(value) {
            super.minValue = min(value, maxValue)
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            super.maxValue = max(value, minValue)
        }

    override var step
        get() = super.step
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

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
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(precision == null || precision >= 0) { "precision must be greater than or equal to 0." }

        require(defaultValue == null || defaultValue in minValue..maxValue) {
            "defaultValue must be between minValue and maxValue."
        }
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

    override fun processValue(value: Float?) = when {
        value == null -> null
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0f -> value

        else -> {
            val minValue = minValue
            val step = step
            val precision = precision

            val value = round((value - minValue) / step) * step + minValue

            if (precision != null) value.preciseRoundBy(precision) else value
        }
    }

    override fun copyFrom(other: ModSetting<Float?>) {
        super.copyFrom(other)

        if (other is NullableFloatModSetting) {
            precision = other.precision
        }
    }
}