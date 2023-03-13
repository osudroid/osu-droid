package com.rian.difficultycalculator.evaluators.standard;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.utils.GameMode;

/**
 * An evaluator for calculating osu!standard flashlight skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class StandardFlashlightEvaluator {
    private StandardFlashlightEvaluator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates the difficulty of memorizing and hitting the current object, based on:
     * <ul>
     *     <li>distance between a number of previous objects and the current object,</li>
     *     <li>the visual opacity of the current object,</li>
     *     <li>the angle made by the current object,</li>
     *     <li>length and speed of the current object (for sliders),</li>
     *     <li>and whether Hidden mod is enabled.</li>
     * </ul>
     *
     * @param current The current object.
     * @param isHiddenMod Whether the Hidden mod is enabled.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, boolean isHiddenMod) {
        // Exclude overlapping objects that can be tapped at once.
        if (current.object instanceof Spinner) {
            return 0;
        }

        double scalingFactor = 52 / current.object.getRadius(GameMode.standard);
        double smallDistNerf = 1;
        double cumulativeStrainTime = 0;
        double result = 0;
        DifficultyHitObject last = current;
        double angleRepeatCount = 0;

        for (int i = 0; i < Math.min(current.index, 10); ++i) {
            DifficultyHitObject currentObject = current.previous(i);

            // Exclude overlapping objects that can be tapped at once.
            if (!(currentObject.object instanceof Spinner)) {
                double jumpDistance = current.object
                        .getStackedPosition(GameMode.standard)
                        .subtract(currentObject.object.getStackedEndPosition(GameMode.standard))
                        .getLength();

                cumulativeStrainTime += last.strainTime;

                // We want to nerf objects that can be easily seen within the Flashlight circle radius.
                if (i == 0) {
                    smallDistNerf = Math.min(1, jumpDistance / 75);
                }

                // We also want to nerf stacks so that only the first object of the stack is accounted for.
                double stackNerf = Math.min(1, currentObject.lazyJumpDistance / scalingFactor / 25);

                // Bonus based on how visible the object is.
                double opacityBonus =
                        1 + 0.4 * (1 - current.opacityAt(currentObject.object.getStartTime(), isHiddenMod, GameMode.standard));

                result += (stackNerf * opacityBonus * scalingFactor * jumpDistance) / cumulativeStrainTime;

                if (!Double.isNaN(currentObject.angle) && !Double.isNaN(current.angle)) {
                    // Objects further back in time should count less for the nerf.
                    if (Math.abs(currentObject.angle - current.angle) < 0.02) {
                        angleRepeatCount += Math.max(0, 1 - 0.1 * i);
                    }
                }
            }

            last = currentObject;
        }

        result = Math.pow(smallDistNerf * result, 2);

        // Additional bonus for Hidden due to there being no approach circles.
        if (isHiddenMod) {
            double hiddenBonus = 0.2;
            result *= 1 + hiddenBonus;
        }

        // Nerf patterns with repeated angles.
        double minAngleMultiplier = 0.2;
        result *= minAngleMultiplier + (1 - minAngleMultiplier) / (angleRepeatCount + 1);

        double sliderBonus = 0;
        if (current.object instanceof Slider) {
            // Invert the scaling factor to determine the true travel distance independent of circle size.
            double pixelTravelDistance = ((Slider) current.object).getLazyTravelDistance() / scalingFactor;

            // Reward sliders based on velocity.
            double minVelocity = 0.5;
            sliderBonus = Math.pow(Math.max(0, pixelTravelDistance / current.travelTime - minVelocity), 0.5);

            // Longer sliders require more memorization.
            sliderBonus *= pixelTravelDistance;

            // Nerf sliders with repeats, as less memorization is required.
            sliderBonus /= ((Slider) current.object).getRepeatCount();
        }

        double sliderMultiplier = 1.3;
        result += sliderBonus * sliderMultiplier;

        return result;
    }
}
