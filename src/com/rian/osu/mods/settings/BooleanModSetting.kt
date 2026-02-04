package com.rian.osu.mods.settings

import kotlinx.serialization.json.*

/**
 * A [ModSetting] that represents a [Boolean] value.
 */
open class BooleanModSetting(
    name: String,
    key: String? = null,
    defaultValue: Boolean,
    orderPosition: Int? = null
) : ModSetting<Boolean>(name, key, null, defaultValue, orderPosition) {
    override fun load(json: JsonObject) {
        if (key != null) {
            value = json[key]?.jsonPrimitive?.booleanOrNull ?: defaultValue
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key != null) {
            builder.put(key, value)
        }
    }
}