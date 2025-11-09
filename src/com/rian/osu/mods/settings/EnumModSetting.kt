package com.rian.osu.mods.settings

import org.json.JSONObject

class EnumModSetting<T : Enum<*>>(
    name: String,
    key: String? = null,
    valueFormatter: ModSetting<T>.(T) -> String = { it.toString() },
    defaultValue: T,

    /**
     * The list of possible values for this [EnumModSetting].
     */
    val entries: List<T> = defaultValue.javaClass.enumConstants?.toList() ?: emptyList()

) : ModSetting<T>(name, key, valueFormatter, defaultValue) {
    override fun load(json: JSONObject) {
        if (key != null) {
            value = entries[json.optInt(key, defaultValue.ordinal)]
        }
    }

    override fun save(json: JSONObject) {
        json.putOpt(key, value.ordinal)
    }
}