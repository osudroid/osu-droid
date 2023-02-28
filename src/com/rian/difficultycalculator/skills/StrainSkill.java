package com.rian.difficultycalculator.skills;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;

import java.util.ArrayList;
import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * Used to processes strain values of difficulty hit objects, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
public abstract class StrainSkill extends Skill {
    /**
     * The current section's strain peak.
     */
    protected double currentSectionPeak;

    /**
     * The strain peaks of each sections.
     */
    protected final ArrayList<Double> strainPeaks = new ArrayList<>();

    private double currentSectionEnd = 0;

    /**
     * @param mods The mods that this skill processes.
     */
    public StrainSkill(EnumSet<GameMod> mods) {
        super(mods);
    }

    @Override
    public void process(DifficultyHitObject current) {
        // The first object doesn't generate a strain, so we begin with an incremented section end
        double sectionLength = 400;
        if (current.index == 0) {
            currentSectionEnd = Math.ceil(current.startTime / sectionLength) * sectionLength;
        }

        while (current.startTime > currentSectionEnd) {
            saveCurrentPeak();
            startNewSectionFrom(currentSectionEnd, current);
            currentSectionEnd += sectionLength;
        }

        currentSectionPeak = Math.max(strainValueAt(current), currentSectionPeak);
        saveToHitObject(current);
    }

    /**
     * Calculates the strain value at the hit object.
     * This value is calculated with or without respect to previous objects.
     *
     * @param current The hit object to calculate.
     * @return The strain value at the hit object.
     */
    protected abstract double strainValueAt(DifficultyHitObject current);

    /**
     * Retrieves the peak strain at a point in time.
     *
     * @param time The time to retrieve the peak strain at.
     * @param current The current hit object.
     * @return The peak strain.
     */
    protected abstract double calculateInitialStrain(double time, DifficultyHitObject current);

    /**
     * Saves the current strain to a hit object.
     *
     * @param current The hit object to save to.
     */
    protected abstract void saveToHitObject(DifficultyHitObject current);

    /**
     * Gets the number of sections with the highest strains, which the peak strain reductions will apply to.
     * This is done in order to decrease their impact on the overall difficulty of the beatmap for this skill.
     */
    protected int getReducedSectionCount() {
        return 10;
    }

    /**
     * Gets the baseline multiplier applied to the section with the biggest strain.
     */
    protected double getReducedSectionBaseline() {
        return 0.75;
    }

    /**
     * Saves the current peak strain level to the list of strain peaks,
     * which will be used to calculate an overall difficulty.
     */
    private void saveCurrentPeak() {
        strainPeaks.add(currentSectionPeak);
    }

    /**
     * Sets the initial strain level for a new section.
     *
     * @param time The beginning of the new section, in milliseconds.
     * @param current The current hit object.
     */
    private void startNewSectionFrom(double time, DifficultyHitObject current) {
        // The maximum strain of the new section is not zero by default.
        // This means we need to capture the strain level at the beginning of the new section, and use that as the initial peak level.
        currentSectionPeak = calculateInitialStrain(time, current);
    }
}
