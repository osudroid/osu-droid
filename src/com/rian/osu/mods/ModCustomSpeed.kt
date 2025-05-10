package com.rian.osu.mods

import org.json.JSONObject

/**
 * Represents the Custom Speed mod.
 *
 * @param trackRateMultiplier The multiplier to apply to the track's playback rate.
 */
class ModCustomSpeed @JvmOverloads constructor(trackRateMultiplier: Float = 1f) : ModRateAdjust(trackRateMultiplier) {
    override val name = "Custom Speed"
    override val acronym = "CS"
    override val description = "Play at any speed you want - slow or fast."
    override val type = ModType.Conversion
    override val isRanked = true

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        trackRateMultiplier = settings.optDouble("rateMultiplier", trackRateMultiplier.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("rateMultiplier", trackRateMultiplier)
    }

    override fun toString() = buildString {
        append(super.toString())
        append(" (%.2fx)".format(trackRateMultiplier))
    }

    override fun deepCopy() = ModCustomSpeed(trackRateMultiplier)
}