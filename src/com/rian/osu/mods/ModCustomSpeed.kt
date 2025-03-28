package com.rian.osu.mods

/**
 * Represents the Custom Speed mod.
 *
 * @param trackRateMultiplier The multiplier to apply to the track's playback rate.
 */
class ModCustomSpeed @JvmOverloads constructor(trackRateMultiplier: Float = 1f) : ModRateAdjust(trackRateMultiplier) {
    override val name = "Custom Speed"
    override val acronym = "CS"
    override val textureNameSuffix = "customspeed"
    override val isRanked = true

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
    override fun deepCopy() = ModCustomSpeed(trackRateMultiplier)
}