package com.rian.difficultycalculator.skills;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;

/**
 * A bare minimal abstract skill for fully custom skill implementations.
 */
public abstract class Skill {
    /**
     * The mods that this skill processes.
     */
    protected final EnumSet<GameMod> mods;

    /**
     * @param mods The mods that this skill processes.
     */
    public Skill(final EnumSet<GameMod> mods) {
        this.mods = mods;
    }

    /**
     * Calculates the strain value of a hit object and stores the value in it.
     * This value is affected by previously processed objects.
     *
     * @param current The hit object to process.
     */
    public abstract void process(DifficultyHitObject current);

    /**
     * Returns the calculated difficulty value representing all hit objects
     * that have been processed up to this point.
     */
    public abstract double difficultyValue();
}
