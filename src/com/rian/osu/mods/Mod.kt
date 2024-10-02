package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.reflect.KClass

/**
 * Represents a mod.
 */
abstract class Mod {
    /**
     * The osu!droid string representation of this [Mod].
     */
    abstract val droidString: String

    /**
     * The acronym of this [Mod].
     */
    abstract val acronym: String

    /**
     * Whether scores with this [Mod] active can be submitted online.
     */
    open val ranked = false

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

        return other.droidString == droidString
    }

    override fun hashCode() = droidString.hashCode()
}