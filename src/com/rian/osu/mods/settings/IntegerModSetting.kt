package com.rian.osu.mods.settings

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
) : NumberModSetting<Int>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition, useManualInput)

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
) : NullableNumberModSetting<Int>(name, key, valueFormatter, defaultValue, minValue, maxValue, step, orderPosition, useManualInput)