package com.rian.difficultycalculator.skills.standard;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.evaluators.standard.StandardRhythmEvaluator;
import com.rian.difficultycalculator.evaluators.standard.StandardSpeedEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * Represents the skill required to press keys or tap with regards to keeping up with the speed at which objects need to be hit.
 */
public class StandardSpeed extends StandardSkill {
    private double currentStrain;
    private double currentRhythm;
    private final ArrayList<Double> objectStrains = new ArrayList<>();
    private final double greatWindow;

    /**
     * @param mods The mods that this skill processes.
     * @param greatWindow The 300 hit window.
     */
    public StandardSpeed(EnumSet<GameMod> mods, double greatWindow) {
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
        currentStrain *= strainDecay(current.deltaTime);
        double skillMultiplier = 1375;
        currentStrain += StandardSpeedEvaluator.evaluateDifficultyOf(current, greatWindow) * skillMultiplier;
        currentRhythm = StandardRhythmEvaluator.evaluateDifficultyOf(current, greatWindow);

        double totalStrain = currentStrain * currentRhythm;

        objectStrains.add(totalStrain);

        return totalStrain;
    }

    @Override
    protected double calculateInitialStrain(double time, DifficultyHitObject current) {
        return currentStrain * currentRhythm * strainDecay(time - current.previous(0).startTime);
    }

    @Override
    protected void saveToHitObject(DifficultyHitObject current) {
        current.tapStrain = currentStrain;
        current.rhythmMultiplier = currentRhythm;
    }

    @Override
    protected double getDifficultyMultiplier() {
        return 1.04;
    }

    @Override
    protected int getReducedSectionCount() {
        return 5;
    }

    private double strainDecay(double ms) {
        return Math.pow(0.3, ms / 1000);
    }
}
