package com.rian.osu.mods

/**
 * Represents the Custom Speed mod. Serves as a container for custom speed multipliers.
 */
class ModCustomSpeed(override val trackRateMultiplier: Float) : ModRateAdjust() {
    override val ranked = true

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
}