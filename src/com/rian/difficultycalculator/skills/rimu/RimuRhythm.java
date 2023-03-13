package com.rian.difficultycalculator.skills.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.rimu.RimuRhythmEvaluator;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;

/**
 * Represents the skill required to properly follow a beatmap's rhythm.
 */
public class RimuRhythm extends RimuSkill {
    private double currentStrain;
    private double currentRhythm;
    private final double greatWindow;

    /**
     * @param mods The mods that this skill processes.
     */
    public RimuRhythm(EnumSet<GameMod> mods, double greatWindow) {
        super(mods);

        this.greatWindow = greatWindow;
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        currentRhythm = RimuRhythmEvaluator.evaluateDifficultyOf(current, greatWindow);

        currentStrain *= strainDecay(current.deltaTime);
        currentStrain += currentRhythm - 1;

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        current.rhythmStrain = currentStrain;
        current.rhythmMultiplier = currentRhythm;
    }

    @Override
    protected int getReducedSectionCount() {
        return 5;
    }

    @Override
    protected double getStarsPerDouble() {
        return 1.75;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.3, ms / 1000);
    }
}
