package com.osudroid.mods.settings

import com.osudroid.mods.IModRequiresBeatmapDifficulty
import com.osudroid.mods.ModDifficultyAdjust
import kotlinx.serialization.json.*

/**
 * A [NullableFloatModSetting] variant for [ModDifficultyAdjust] that embeds the beatmap's original difficulty value
 * alongside the user's adjusted value in the serialized format:
 *
 * ```json
 * {"adjusted": 7.0, "original": 4.0}
 * ```
 *
 * When [originalValue] is non-null, the score multiplier can be computed from the serialized format alone, without a
 * beatmap lookup. When `null` (old data or beatmap absent at migration time), the caller must invoke
 * [IModRequiresBeatmapDifficulty.applyFromBeatmapDifficulty] first.
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
    /**
     * The beatmap's original value for this setting, populated by [IModRequiresBeatmapDifficulty.applyFromBeatmapDifficulty].
     *
     * Non-null means the score multiplier is self-contained; `null` means a beatmap lookup is needed.
     */
    var originalValue: Float? = null

    override fun load(json: JsonObject) {
        if (key == null) {
            return
        }

        val element = json[key]

        if (element is JsonObject) {
            value = element["adjusted"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.floatOrNull

            val original = element["original"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.floatOrNull
            originalValue = original

            if (original != null) {
                defaultValue = original
            }
        } else {
            // This is in old scalar format (or null / JsonNull). Delegate to parent for backward compatibility.
            super.load(json)

            originalValue = null
        }
    }

    override fun save(builder: JsonObjectBuilder) {
        if (key == null || value == null) {
            return
        }

        builder.put(key, buildJsonObject {
            put("adjusted", value!!)

            val original = originalValue

            if (original != null) {
                put("original", original)
            } else {
                put("original", JsonNull)
            }
        })
    }

    override fun copyFrom(other: ModSetting<Float?>) {
        super.copyFrom(other)

        if (other is DifficultyAdjustModSetting) {
            originalValue = other.originalValue
        }
    }
}
