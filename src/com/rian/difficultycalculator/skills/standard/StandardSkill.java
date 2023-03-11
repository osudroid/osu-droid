package com.rian.difficultycalculator.skills.standard;

import com.rian.difficultycalculator.math.Interpolation;
import com.rian.difficultycalculator.math.MathUtils;
import com.rian.difficultycalculator.skills.StrainSkill;

import java.util.EnumSet;
import java.util.List;

import main.osu.game.mods.GameMod;


/**
 * Used to processes strain values of difficulty hit objects, keep track of strain levels caused by the processed objects
 * and to calculate a final difficulty value representing the difficulty of hitting all the processed objects.
 */
public abstract class StandardSkill extends StrainSkill {
    /**
     * @param mods The mods that this skill processes.
     */
    public StandardSkill(EnumSet<GameMod> mods) {
        super(mods);
    }

    @Override
    public double difficultyValue() {
        List<Double> strains = strainPeaks.subList(0, strainPeaks.size() - 1);
        strains.sort((d1, d2) -> Double.compare(d2, d1));

        if (getReducedSectionCount() > 0) {
            // We are reducing the highest strains first to account for extreme difficulty spikes.
            for (int i = 0; i < Math.min(strains.size(), getReducedSectionCount()); ++i) {
                double scale = Math.log10(Interpolation.linear(1, 10, MathUtils.clamp((double) i / getReducedSectionCount(), 0, 1)));

                strains.set(i, strains.get(i) * Interpolation.linear(getReducedSectionBaseline(), 1, scale));
            }

            strains.sort((d1, d2) -> Double.compare(d2, d1));
        }

        // Difficulty is the weighted sum of the highest strains from every section.
        // We're sorting from highest to lowest strain.
        double difficulty = 0;
        double weight = 1;

        for (double strain : strains) {
            double addition = strain * weight;

            if (difficulty + addition == difficulty) {
                break;
            }

            difficulty += addition;
            weight *= getDecayWeight();
        }

        return difficulty * getDifficultyMultiplier();
    }

    /**
     * Gets the final multiplier to be applied to the final difficulty value after all other calculations.
     */
    protected double getDifficultyMultiplier() {
        return 1.06;
    }

    /**
     * Gets the weight by which each strain value decays.
     */
    protected double getDecayWeight() {
        return 0.9;
    }
}
