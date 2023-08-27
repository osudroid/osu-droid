package com.rian.difficultycalculator.skills;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.AimEvaluator;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;


/**
 * Represents the skill required to correctly aim at every object in the map with a uniform circle size and normalized distances.
 */
public class Aim extends StrainSkill {
    private final boolean withSliders;
    private double currentStrain;

    /**
     * @param mods The mods that this skill processes.
     * @param withSliders Whether to consider sliders in the calculation.
     */
    public Aim(EnumSet<GameMod> mods, boolean withSliders) {
        super(mods);

        this.withSliders = withSliders;
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        currentStrain *= strainDecay(current.deltaTime);
        double skillMultiplier = 23.55;
        currentStrain += AimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplier;

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        if (withSliders) {
            current.aimStrainWithSliders = currentStrain;
        } else {
            current.aimStrainWithoutSliders = currentStrain;
        }
    }

    private double strainDecay(double ms) {
        return Math.pow(0.15, ms / 1000);
    }
}
