package com.rian.osu.difficultycalculator.skills

import com.rian.osu.difficultycalculator.DifficultyHitObject
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*

/**
 * A bare minimal abstract skill for fully custom skill implementations.
 */
abstract class Skill(
    /**
     * The mods that this skill processes.
     */
    protected val mods: EnumSet<GameMod>
) {
    /**
     * Calculates the strain value of a hit object and stores the value in it.
     * This value is affected by previously processed objects.
     *
     * @param current The hit object to process.
     */
    abstract fun process(current: DifficultyHitObject)

    /**
     * Returns the calculated difficulty value representing all hit objects
     * that have been processed up to this point.
     */
    abstract fun difficultyValue(): Double
}
