package com.rian.osu.difficulty.calculator

import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHalfTime
import com.rian.osu.mods.ModNightCore

/**
 * A class for specifying parameters for difficulty calculation.
 */
class DifficultyCalculationParameters {
    /**
     * The mods to calculate for.
     */
    var mods = mutableListOf<Mod>()

    /**
     * The custom speed multiplier to calculate for.
     */
    var customSpeedMultiplier = 1f

    /**
     * The overall speed multiplier to calculate for.
     */
    val totalSpeedMultiplier: Float
        get() {
            var speedMultiplier = customSpeedMultiplier

            if (mods.any { it is ModDoubleTime || it is ModNightCore }) {
                speedMultiplier *= 1.5f
            }

            if (mods.any { it is ModHalfTime }) {
                speedMultiplier *= 0.75f
            }

            return speedMultiplier
        }

    /**
     * Copies this instance to another instance.
     *
     * @return The copied instance.
     */
    fun copy() = DifficultyCalculationParameters().also {
        it.mods.addAll(mods)
        it.customSpeedMultiplier = customSpeedMultiplier
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is DifficultyCalculationParameters) {
            return false
        }

        if (customSpeedMultiplier != other.customSpeedMultiplier) {
            return false
        }

        return mods.size == other.mods.size && mods.containsAll(other.mods)
    }

    override fun hashCode(): Int {
        var result = mods.hashCode()
        result = 31 * result + customSpeedMultiplier.hashCode()

        return result
    }
}