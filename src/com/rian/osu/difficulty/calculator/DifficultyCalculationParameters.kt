package com.rian.osu.difficulty.calculator

import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModNightCore

/**
 * A class for specifying parameters for difficulty calculation.
 */
class DifficultyCalculationParameters @JvmOverloads constructor(
    /**
     * The mods to calculate for.
     */
    @JvmField
    var mods: MutableList<Mod> = mutableListOf(),

    /**
     * The custom speed multiplier to calculate for.
     */
    @JvmField
    var customSpeedMultiplier: Float = 1f,

    /**
     * Whether to enforce old statistics.
     *
     * Some [Mod]s behave differently with this flag. For example, [ModNightCore] will apply a 1.39 rate multiplier
     * instead of 1.5 when this is `true`. **Never set this flag to `true` unless you know what you are doing.**
     */
    @JvmField
    var oldStatistics: Boolean = false
) {
    /**
     * Copies this [DifficultyCalculationParameters] to another [DifficultyCalculationParameters].
     *
     * @return The copied [DifficultyCalculationParameters].
     */
    fun copy() = DifficultyCalculationParameters(
        customSpeedMultiplier = customSpeedMultiplier, oldStatistics = oldStatistics
    ).also { it.mods.addAll(mods) }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is DifficultyCalculationParameters) {
            return false
        }

        if (customSpeedMultiplier != other.customSpeedMultiplier || oldStatistics != other.oldStatistics) {
            return false
        }

        return mods.size == other.mods.size && mods.containsAll(other.mods)
    }

    override fun hashCode(): Int {
        var result = mods.hashCode()

        result = 31 * result + customSpeedMultiplier.hashCode()
        result = 31 * result + oldStatistics.hashCode()

        return result
    }
}