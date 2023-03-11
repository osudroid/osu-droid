package com.rian.difficultycalculator.skills.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.rimu.RimuFlashlightEvaluator;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
public class RimuFlashlight extends RimuSkill {
    private double currentStrain;
    private final boolean hasHidden;
    private final boolean withSliders;

    /**
     * @param mods The mods that this skill processes.
     * @param withSliders Whether to consider sliders in the calculation.
     */
    public RimuFlashlight(EnumSet<GameMod> mods, boolean withSliders) {
        super(mods);

        hasHidden = mods.contains(GameMod.MOD_HIDDEN);
        this.withSliders = withSliders;
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        currentStrain *= strainDecay(current.deltaTime);
        double skillMultiplier = 0.125;
        currentStrain += RimuFlashlightEvaluator.evaluateDifficultyOf(current, hasHidden, withSliders) * skillMultiplier;

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        if (withSliders) {
            current.flashlightStrainWithSliders = currentStrain;
        } else {
            current.flashlightStrainWithoutSliders = currentStrain;
        }
    }

    @Override
    protected int getReducedSectionCount() {
        return 0;
    }

    @Override
    protected double getReducedSectionBaseline() {
        return 1;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.15, ms / 1000);
    }
}
