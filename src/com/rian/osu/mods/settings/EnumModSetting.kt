package com.rian.osu.mods.settings

class EnumModSetting<T : Enum<*>>(
    name: String,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,

    /**
     * The list of possible values for this [EnumModSetting].
     */
    val entries: List<T> = defaultValue.javaClass.enumConstants?.toList() ?: emptyList()

) : ModSetting<T>(name, valueFormatter, defaultValue)