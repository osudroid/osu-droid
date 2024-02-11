package com.rian.osu.difficultycalculator.calculator

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*

/**
 * A class for specifying parameters for difficulty calculation.
 */
class DifficultyCalculationParameters {
    /**
     * The mods to calculate for.
     */
    var mods: EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java)

    /**
     * The custom speed multiplier to calculate for.
     */
    var customSpeedMultiplier = 1f

    /**
     * The custom CS setting to calculate for. Set to `Float.NaN` to disable.
     */
    var customCS = Float.NaN

    /**
     * The custom AR setting to calculate for. Set to `Float.NaN` to disable.
     */
    var customAR = Float.NaN

    /**
     * The custom OD setting to calculate for. Set to `Float.NaN` to disable.
     */
    var customOD = Float.NaN

    val totalSpeedMultiplier: Float
        /**
         * Retrieves the overall speed multiplier to calculate for.
         */
        get() {
            var speedMultiplier = customSpeedMultiplier

            if (mods.contains(GameMod.MOD_DOUBLETIME) || mods.contains(GameMod.MOD_NIGHTCORE)) {
                speedMultiplier *= 1.5f
            }

            if (mods.contains(GameMod.MOD_HALFTIME)) {
                speedMultiplier *= 0.75f
            }

            return speedMultiplier
        }

    /**
     * Whether custom CS is used in this parameter.
     */
    fun isCustomCS() = !customCS.isNaN()

    /**
     * Whether custom AR is used in this parameter.
     */
    fun isCustomAR() = !customAR.isNaN()

    /**
     * Whether custom OD is used in this parameter.
     */
    fun isCustomOD() = !customOD.isNaN()

    /**
     * Copies this instance to another instance.
     *
     * @return The copied instance.
     */
    fun copy() = DifficultyCalculationParameters().also {
        it.mods = EnumSet.copyOf(mods)
        it.customCS = customCS
        it.customAR = customAR
        it.customOD = customOD
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

        if (isCustomCS() != other.isCustomCS() || isCustomAR() != other.isCustomAR() || isCustomOD() != other.isCustomOD()
        ) {
            return false
        }

        if (isCustomCS() && other.isCustomCS() && customCS != other.customCS) {
            return false
        }

        if (isCustomAR() && other.isCustomAR() && customAR != other.customAR) {
            return false
        }

        if (isCustomOD() && other.isCustomOD() && customOD != other.customOD) {
            return false
        }

        return mods.size == other.mods.size && mods.containsAll(other.mods)
    }

    override fun hashCode(): Int {
        var result = mods.hashCode()
        result = 31 * result + customSpeedMultiplier.hashCode()
        result = 31 * result + customCS.hashCode()
        result = 31 * result + customAR.hashCode()
        result = 31 * result + customOD.hashCode()

        return result
    }
}