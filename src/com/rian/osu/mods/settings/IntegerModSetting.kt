package com.rian.osu.mods.settings

import kotlin.math.*
import kotlinx.serialization.json.*

/**
 * A [ModSetting] that represents an [Int] value with range constraints.
 */
open class IntegerModSetting(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<Int>.(Int) -> String = { it.toString() },
    defaultValue: Int,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [IntegerModSetting] manually.
     */
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<Int>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition) {
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
            if (value > maxValue) {
                throw IllegalArgumentException("minValue cannot be greater than maxValue.")
            }

            super.minValue = value
        }

    override var maxValue
        get() = super.maxValue
        set(value) {
            if (value < minValue) {
                throw IllegalArgumentException("maxValue cannot be less than minValue.")
            }

            super.maxValue = value
        }

    override var step
        get() = super.step
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("step cannot be less than 0.")
            }

            super.step = value
        }

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
    }

    override fun load(json: JsonObject) {
        if (key != null) {
            value = json[key]?.jsonPrimitive?.intOrNull ?: defaultValue
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key != null) {
            builder.put(key, value)
        }
    }

    override fun processValue(value: Int) = when {
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0 -> value
        else -> (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()
    }
}

/**
 * A [ModSetting] that represents a nullable [Int] value with range constraints.
 */
open class NullableIntegerModSetting(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<Int?>.(Int?) -> String = { it.toString() },
    defaultValue: Int?,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    step: Int = 1,
    orderPosition: Int? = null,

    /**
     * Whether to allow the user to input the value of this [NullableIntegerModSetting] manually.
     */
    val useManualInput: Boolean = false

) : RangeConstrainedModSetting<Int?>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition) {
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

    init {
        require(minValue <= maxValue) { "minValue must be less than or equal to maxValue." }
        require(step >= 0f) { "step must be greater than or equal to 0." }
        require(defaultValue == null || defaultValue in minValue..maxValue) { "defaultValue must be between minValue and maxValue." }
    }

    override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]

        value =
            if (element is JsonNull) null
            else element?.jsonPrimitive?.intOrNull ?: defaultValue
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

    override fun processValue(value: Int?) = when {
        value == null -> null
        value < minValue -> minValue
        value > maxValue -> maxValue
        step == 0 -> value

        else -> (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()
    }
}