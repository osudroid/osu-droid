package com.osudroid.mods.settings

import com.osudroid.mods.ModDifficultyAdjust
import kotlinx.serialization.json.*

/**
 * A [NullableFloatModSetting] variant for [ModDifficultyAdjust] that accepts the beatmap's original difficulty value
 * alongside the user's adjusted value from the legacy serialized format:
 *
 * ```json
 * {"adjusted": 7.0, "original": 4.0}
 * ```
 *
 * Old scalar values (`"cs": 7.0`) are accepted on [load] for backward compatibility.
 */
class DifficultyAdjustModSetting(
    name: String,
    key: String,
    valueFormatter: ModSetting<Float?>.(Float?) -> String,
    minValue: Float,
    maxValue: Float,
    step: Float,
    precision: Int,
    orderPosition: Int,
    useManualInput: Boolean = false
) : NullableFloatModSetting(
    name = name,
    key = key,
    valueFormatter = valueFormatter,
    defaultValue = null,
    minValue = minValue,
    maxValue = maxValue,
    step = step,
    precision = precision,
    orderPosition = orderPosition,
    useManualInput = useManualInput
) {
    // "Default" means no override is active (value == null == initialValue), regardless of what
    // defaultValue currently holds (which may be set to the beatmap's difficulty for UI hint display).
    override val isDefault
        get() = value == initialValue

    override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]

        if (element is JsonObject) {
            // TODO: Remove this branch in a future migration once all legacy {"adjusted","original"} scores are gone.
            value = element["adjusted"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.floatOrNull

            val original = element["original"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.floatOrNull

            if (original != null) {
                defaultValue = original
            }
        } else {
            // This is in old scalar format (or null / JsonNull). Delegate to parent for backward compatibility.
            super.load(json)
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key == null || value == null) {
            return
        }

        builder.put(key, value!!)
    }
}
