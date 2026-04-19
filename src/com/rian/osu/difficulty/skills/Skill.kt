package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.mods.Mod

/**
 * A bare minimal abstract skill for fully custom skill implementations.
 */
abstract class Skill<in TObject : DifficultyHitObject>(
    /**
     * The [Mod]s that this skill processes.
     */
    protected val mods: Iterable<Mod>
) {
    /**
     * Difficulties of [TObject]s, populated by [process].
     */
    val objectDifficulties = mutableListOf<Double>()

    /**
     * Calculates the strain value of a hit object and stores the value in it.
     * This value is affected by previously processed objects.
     *
     * @param current The hit object to process.
     */
    fun process(current: TObject) {
        if (current.index < 0) {
            return
        }

        objectDifficulties += processInternal(current)
    }

    /**
     * Returns the calculated difficulty value representing all hit objects
     * that have been processed up to this point.
     */
    abstract fun difficultyValue(): Double

    /**
     * Calculates the difficulty value of a [TObject].
     *
     * @param current The [TObject] to calculate the difficulty of.
     * @return The difficulty of [current].
     */
    protected abstract fun processInternal(current: TObject): Double
}
