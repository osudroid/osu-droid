package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.reflect.KClass

/**
 * Represents a mod.
 */
abstract class Mod {
    /**
     * Whether scores with this [Mod] active can be submitted online.
     */
    open val isRanked = false

    /**
     * Whether this [Mod] can be specified as a "required" [Mod] in multiplayer context.
     */
    open val isValidForMultiplayer = true

    /**
     * Whether this [Mod] can be specified as a "free" or "allowed" [Mod] in multiplayer context.
     */
    open val isValidForMultiplayerAsFreeMod = true

    /**
     * The [Mod]s this [Mod] cannot be enabled with.
     */
    open val incompatibleMods = emptyArray<KClass<out Mod>>()

    /**
     * Calculates the score multiplier for this [Mod] with the given [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to calculate the score multiplier for.
     * @return The score multiplier for this [Mod] with the given [BeatmapDifficulty].
     */
    open fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1f

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Mod) {
            return false
        }

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        var result = isRanked.hashCode()

        result = 31 * result + incompatibleMods.contentHashCode()

        if (this is IModUserSelectable) {
            result = 31 * result + droidChar.hashCode()
            result = 31 * result + acronym.hashCode()
        }

        return result
    }
}