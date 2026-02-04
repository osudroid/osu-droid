package com.rian.osu.mods.settings

import kotlinx.serialization.json.*

/**
 * A [ModSetting] that represents an [Enum] value.
 */
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
    override fun load(json: JsonObject) {
        if (key != null) {
            value = entries[json[key]?.jsonPrimitive?.intOrNull ?: defaultValue.ordinal]
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key != null) {
            builder.put(key, value.ordinal)
        }
    }
}