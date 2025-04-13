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
    override val type = ModType.Conversion
    override val textureNameSuffix = "customspeed"
    override val isRanked = true

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        trackRateMultiplier = settings.optDouble("rateMultiplier", trackRateMultiplier.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("rateMultiplier", trackRateMultiplier)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModCustomSpeed) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode() = super.hashCode()

    override fun toString() = buildString {
        append(super.toString())
        append(" (%.2fx)".format(trackRateMultiplier))
    }

    override fun deepCopy() = ModCustomSpeed(trackRateMultiplier)
}