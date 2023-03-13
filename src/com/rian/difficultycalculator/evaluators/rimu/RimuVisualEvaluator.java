package com.rian.difficultycalculator.evaluators.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.math.MathUtils;
import com.rian.difficultycalculator.utils.GameMode;

/**
 * An evaluator for calculating rimu! visual skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class RimuVisualEvaluator {
    private RimuVisualEvaluator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates the difficulty of reading the current object, based on:
     * <ul>
     *     <li>note density of the current object,</li>
     *     <li>overlapping factor of the current object,</li>
     *     <li>the preempt time of the current object,</li>
     *     <li>the visual opacity of the current object,</li>
     *     <li>the velocity of the current object if it's a slider,</li>
     *     <li>past objects' velocity if they are sliders,</li>
     *     <li>and whether the Hidden mod is enabled.</li>
     * </ul>
     *
     * @param current The current object.
     * @param isHiddenMod Whether the Hidden mod is enabled.
     * @param withSliders Whether to take slider difficulty into account.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, boolean isHiddenMod, boolean withSliders) {
        // Exclude overlapping objects that can be tapped at once.
        if (current.object instanceof Spinner || current.isOverlapping(true)) {
            return 0;
        }

        // Start with base density and give global bonus for Hidden.
        // Add density caps for sanity.
        double strain;

        if (isHiddenMod) {
            strain = Math.min(30, Math.pow(current.noteDensity, 3));
        } else {
            strain = Math.min(20, Math.pow(current.noteDensity, 2));
        }

        // Bonus based on how visible the object is.
        for (int i = 0; i < Math.min(current.index, 10); ++i) {
            DifficultyHitObject previous = current.previous(i);

            // Exclude overlapping objects that can be tapped at once.
            if (previous.object instanceof Spinner || previous.isOverlapping(true)) {
                continue;
            }

            double realDeltaTime = current.object.getStartTime();

            if (previous.object instanceof HitObjectWithDuration) {
                realDeltaTime -= ((HitObjectWithDuration) previous.object).getEndTime();
            } else {
                realDeltaTime -= previous.object.getStartTime();
            }

            // Do not consider objects that don't fall under time preempt.
            if (realDeltaTime > current.baseTimePreempt) {
                break;
            }

            strain += (1 - current.opacityAt(previous.object.getStartTime(), isHiddenMod, GameMode.rimu)) / 4;
        }

        // Scale the value with overlapping factor.
        strain /= 10 * (1 + current.overlappingFactor);

        if (current.timePreempt < 400) {
            // Give bonus for AR higher than 10.33.
            strain += Math.pow(400 - current.timePreempt, 1.3) / 100;
        }

        if (current.object instanceof Slider && withSliders) {
            double scalingFactor = 50 / current.object.getRadius(GameMode.rimu);

            // Reward sliders based on velocity.
            strain +=
                    // Avoid over-buffing extremely fast sliders.
                    Math.min(6, current.velocity * 1.5) *
                    // Scale with distance travelled to avoid over-buffing fast sliders with short distance.
                    Math.min(1, current.travelDistance / scalingFactor / 125);

            double cumulativeStrainTime = 0;

            // Reward for velocity changes based on last few sliders.
            for (int i = 0; i < Math.min(current.index, 4); ++i) {
                DifficultyHitObject last = current.previous(i);
                cumulativeStrainTime += last.strainTime;

                // Exclude overlapping objects that can be tapped at once.
                if (!(last.object instanceof Slider) || last.isOverlapping(true)) {
                    continue;
                }

                strain +=
                        // Avoid over-buffing extremely fast velocity changes.
                        Math.min(10, 2.5 * Math.abs(current.velocity - last.velocity)) *
                        // Scale with distance travelled to avoid over-buffing fast sliders with short distance.
                        Math.min(1, last.travelDistance / scalingFactor / 100) *
                        // Scale with cumulative strain time to avoid over-buffing past sliders.
                        Math.min(1, 300 / cumulativeStrainTime);
            }
        }

        // Reward for rhythm changes.
        if (current.rhythmMultiplier > 1) {
            double rhythmBonus = (current.rhythmMultiplier - 1) / 20;

            // Rhythm changes are harder to read in Hidden.
            // Add additional bonus for Hidden.
            if (isHiddenMod) {
                rhythmBonus += (current.rhythmMultiplier - 1) / 25;
            }

            // Rhythm changes are harder to read when objects are stacked together.
            // Scale rhythm bonus based on the stack of past objects.
            double diameter = 2 * current.object.getRadius(GameMode.rimu);
            double cumulativeStrainTime = 0;

            for (int i = 0; i < Math.min(current.index, 5); ++i) {
                DifficultyHitObject previous = current.previous(i);

                // Exclude overlapping objects that can be tapped at once.
                if (previous.object instanceof Spinner || previous.isOverlapping(true)) {
                    continue;
                }

                double jumpDistance = current.object
                        .getStackedPosition(GameMode.rimu)
                        .getDistance(
                                previous.object.getStackedEndPosition(GameMode.rimu)
                        );

                cumulativeStrainTime += previous.strainTime;

                rhythmBonus +=
                        // Scale the bonus with diameter.
                        MathUtils.clamp((0.5 - jumpDistance / diameter) / 10, 0, 0.05) *
                        // Scale with cumulative strain time to avoid over-buffing past objects.
                        Math.min(1, 300 / cumulativeStrainTime);

                // Give a larger bonus for Hidden.
                if (isHiddenMod) {
                    rhythmBonus += (1 - current.opacityAt(previous.object.getStartTime(), true, GameMode.rimu)) / 20;
                }
            }

            strain += rhythmBonus;
        }

        return strain;
    }
}
