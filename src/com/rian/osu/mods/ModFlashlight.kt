package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Flashlight mod.
 */
class ModFlashlight : Mod(), IModUserSelectable {
    override val droidChar = 'i'
    override val acronym = "FL"
    override val textureNameSuffix = "flashlight"
    override val enum = GameMod.MOD_FLASHLIGHT
    override val isRanked
        get() = followDelay == DEFAULT_FOLLOW_DELAY

    /**
     * The amount of seconds until the flashlight reaches the cursor.
     */
    var followDelay = DEFAULT_FOLLOW_DELAY

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f
    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModFlashlight) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()

        result = 31 * result + followDelay.hashCode()

        return result
    }

    override fun deepCopy() = ModFlashlight().also { it.followDelay = followDelay }

    companion object {
        /**
         * The default amount of seconds until the flashlight reaches the cursor.
         */
        const val DEFAULT_FOLLOW_DELAY = 0.12f
    }
}