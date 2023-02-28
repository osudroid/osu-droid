package com.rian.difficultycalculator.skills.rimu;

import com.rian.difficultycalculator.math.Interpolation;
import com.rian.difficultycalculator.math.MathUtils;
import com.rian.difficultycalculator.skills.StrainSkill;

import java.util.EnumSet;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * Used to processes strain values of difficulty hitobjects, keep track of strain levels caused by the processed objects
 * and to calculate a final difficulty value representing the difficulty of hitting all the processed objects.
 */
public abstract class RimuSkill extends StrainSkill {
    /**
     * @param mods The mods that this skill processes.
     */
    public RimuSkill(EnumSet<GameMod> mods) {
        super(mods);
    }

    @Override
    public double difficultyValue() {
        List<Double> strains = strainPeaks.subList(0, strainPeaks.size() - 1);

        if (getReducedSectionCount() > 0) {
            strains.sort((d1, d2) -> Double.compare(d2, d1));

            // We are reducing the highest strains first to account for extreme difficulty spikes.
            for (int i = 0; i < Math.min(strains.size(), getReducedSectionCount()); ++i) {
                double scale = Math.log10(Interpolation.linear(1, 10, MathUtils.clamp((double) i / getReducedSectionCount(), 0, 1)));

                strains.set(i, strains.get(i) * Interpolation.linear(getReducedSectionBaseline(), 1, scale));
            }
        }

        // Math here preserves the property that two notes of equal difficulty x, we have their summed difficulty = x * starsPerDouble.
        // This also applies to two sets of notes with equal difficulty.
        double difficulty = 0;

        for (double strain : strains) {
            difficulty += Math.pow(strain, 1 / Math.log(getStarsPerDouble()) / Math.log(2));
        }

        return Math.pow(difficulty, Math.log(getStarsPerDouble()) / Math.log(2));
    }

    /**
     * Gets the bonus multiplier that is given for a sequence of notes of equal difficulty.
     */
    protected double getStarsPerDouble() {
        return 1.05;
    }
}
