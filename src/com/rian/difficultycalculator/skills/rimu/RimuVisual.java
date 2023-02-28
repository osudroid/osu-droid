package com.rian.difficultycalculator.skills.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.rimu.RimuVisualEvaluator;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * Represents the skill required to read every object in a beatmap.
 */
public class RimuVisual extends RimuSkill {
    private double currentStrain;

    private final boolean hasHidden;
    private final boolean withSliders;

    /**
     * @param mods The mods that this skill processes.
     * @param withSliders Whether to consider sliders in the calculation.
     */
    public RimuVisual(EnumSet<GameMod> mods, boolean withSliders) {
        super(mods);

        hasHidden = mods.contains(GameMod.MOD_HIDDEN);
        this.withSliders = withSliders;
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        currentStrain *= strainDecay(current.deltaTime);
        double skillMultiplier = 10;
        currentStrain += RimuVisualEvaluator.evaluateDifficultyOf(current, hasHidden, withSliders) * skillMultiplier;

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        if (withSliders) {
            current.visualStrainWithSliders = currentStrain;
        } else {
            current.visualStrainWithoutSliders = currentStrain;
        }
    }

    @Override
    protected double getStarsPerDouble() {
        return 1.025;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.15, ms / 1000);
    }
}
