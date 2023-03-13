package com.rian.difficultycalculator.skills.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.rimu.RimuTapEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import main.osu.game.mods.GameMod;

/**
 * Represents the skill required to tap with regards to keeping up with the speed at which objects need to be hit.
 */
public class RimuTap extends RimuSkill {
    private double currentStrain;
    private double currentOriginalStrain;
    private final ArrayList<Double> objectStrains = new ArrayList<>();
    private final double greatWindow;

    /**s
     * @param mods The mods that this kill processes.
     * @param greatWindow The 300 hit window.
     */
    public RimuTap(EnumSet<GameMod> mods, double greatWindow) {
        super(mods);

        this.greatWindow = greatWindow;
    }

    /**
     * Calculates the number of clickable objects weighted by difficulty.
     */
    public double relevantNoteCount() {
        if (objectStrains.size() == 0)
            return 0;

        double maxStrain = Collections.max(objectStrains);

        if (maxStrain == 0)
            return 0;

        double relevantNoteCount = 0;

        for (double strain : objectStrains) {
            relevantNoteCount += 1 / (1 + Math.exp(-(strain / maxStrain * 12 - 6)));
        }

        return relevantNoteCount;
    }

    @Override
    protected double strainValueAt(DifficultyHitObject current) {
        double decay = strainDecay(current.deltaTime);
        double skillMultiplier = 1375;

        currentStrain *= decay;
        currentStrain += RimuTapEvaluator.evaluateDifficultyOf(current, greatWindow, true) * skillMultiplier;
        currentStrain *= current.rhythmMultiplier;

        currentOriginalStrain *= decay;
        currentOriginalStrain += RimuTapEvaluator.evaluateDifficultyOf(current, greatWindow, false) * skillMultiplier;
        currentOriginalStrain *= current.rhythmMultiplier;

        objectStrains.add(currentStrain);

        return currentStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        current.tapStrain = currentStrain;
        current.originalTapStrain = currentOriginalStrain;
    }

    @Override
    protected double getStarsPerDouble() {
        return 1.1;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.3, ms / 1000);
    }
}
