package com.rian.difficultycalculator.skills;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.FlashlightEvaluator;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
public class Flashlight extends StrainSkill {
    private double currentStrain;
    private final boolean hasHidden;

    /**
     * @param mods The mods that this skill processes.
     */
    public Flashlight(EnumSet<GameMod> mods) {
        super(mods);

        hasHidden = mods.contains(GameMod.MOD_HIDDEN);
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        currentStrain *= strainDecay(current.deltaTime);
        double skillMultiplier = 0.052;
        currentStrain += FlashlightEvaluator.evaluateDifficultyOf(current, hasHidden) * skillMultiplier;

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        current.flashlightStrain = currentStrain;
    }

    @Override
    protected int getReducedSectionCount() {
        return 0;
    }

    @Override
    protected double getReducedSectionBaseline() {
        return 1;
    }

    @Override
    protected double getDecayWeight() {
        return 1;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.15, ms / 1000);
    }
}
