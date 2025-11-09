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
    useManualInput: Boolean = false
) : NumberModSetting<Int>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition, useManualInput) {
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

    override fun snapToStep(value: Int) = (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()
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
    useManualInput: Boolean = false
) : NullableNumberModSetting<Int>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition, useManualInput) {
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

    override fun snapToStep(value: Int) = (round((value - minValue) / step.toFloat()) * step + minValue).roundToInt()
}