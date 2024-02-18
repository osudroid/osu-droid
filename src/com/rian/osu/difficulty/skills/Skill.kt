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
    protected val mods: List<Mod>
) {
    /**
     * Calculates the strain value of a hit object and stores the value in it.
     * This value is affected by previously processed objects.
     *
     * @param current The hit object to process.
     */
    abstract fun process(current: TObject)

    /**
     * Returns the calculated difficulty value representing all hit objects
     * that have been processed up to this point.
     */
    abstract fun difficultyValue(): Double
}
