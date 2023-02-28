package com.rian.difficultycalculator.evaluators.standard;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.math.MathUtils;

/**
 * An evaluator for calculating osu!standard aim skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class StandardAimEvaluator {
    private static final double wideAngleMultiplier = 1.5;
    private static final double acuteAngleMultiplier = 1.95;
    private static final double sliderMultiplier = 1.35;
    private static final double velocityChangeMultiplier = 0.75;

    private StandardAimEvaluator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates the difficulty of aiming the current object, based on:
     * <ul>
     *     <li>cursor velocity to the current object,</li>
     *     <li>angle difficulty,</li>
     *     <li>sharp velocity increases,</li>
     *     <li>and slider difficulty.</li>
     * </ul>
     *
     * @param current The current object.
     * @param withSliders Whether to take slider difficulty into account.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, boolean withSliders) {
        // Exclude overlapping objects that can be tapped at once.
        if (current.object instanceof Spinner || current.isOverlapping(true)) {
            return 0;
        }

        DifficultyHitObject last = current.previous(0);

        if (current.index <= 1 || (last != null && last.object instanceof Spinner)) {
            return 0;
        }

        DifficultyHitObject lastLast = current.previous(1);

        // Calculate the velocity to the current hit object, which starts with a base distance / time assuming the last object is a hitcircle.
        double currentVelocity = current.lazyJumpDistance / current.strainTime;

        // But if the last object is a slider, then we extend the travel velocity through the slider into the current object.
        if (last.object instanceof Slider && withSliders) {
            // Calculate the slider velocity from slider head to slider end.
            double travelVelocity = last.travelDistance / last.travelTime;

            // Calculate the movement velocity from slider end to current object.
            double movementVelocity = current.minimumJumpTime != 0
                    ? current.minimumJumpDistance / current.minimumJumpTime
                    : 0;

            // Take the larger total combined velocity.
            currentVelocity = Math.max(currentVelocity, movementVelocity + travelVelocity);
        }

        // As above, do the same for the previous hitobject.
        double prevVelocity = last.lazyJumpDistance / last.strainTime;

        if (lastLast.object instanceof Slider && withSliders) {
            double travelVelocity = lastLast.travelDistance / lastLast.travelTime;
            double movementVelocity = last.minimumJumpTime != 0
                    ? last.minimumJumpDistance / last.minimumJumpTime
                    : 0;

            prevVelocity = Math.max(prevVelocity, movementVelocity + travelVelocity);
        }

        double wideAngleBonus = 0;
        double acuteAngleBonus = 0;
        double sliderBonus = 0;
        double velocityChangeBonus = 0;

        // Start strain with regular velocity.
        double strain = currentVelocity;

        if (
            // If rhythms are the same.
                Math.max(current.strainTime, last.strainTime) <
                        1.25 * Math.min(current.strainTime, last.strainTime) &&
                        !Double.isNaN(current.angle) &&
                        !Double.isNaN(last.angle) &&
                        !Double.isNaN(lastLast.angle)
        ) {
            // Rewarding angles, take the smaller velocity as base.
            double angleBonus = Math.min(currentVelocity, prevVelocity);

            wideAngleBonus = calculateWideAngleBonus(current.angle);
            acuteAngleBonus = calculateAcuteAngleBonus(current.angle);

            // Only buff deltaTime exceeding 300 BPM 1/2.
            if (current.strainTime > 100) {
                acuteAngleBonus = 0;
            } else {
                acuteAngleBonus *=
                        // Multiply by previous angle, we don't want to buff unless this is a wiggle type pattern.
                        calculateAcuteAngleBonus(last.angle) *
                                // The maximum velocity we buff is equal to 125 / strainTime.
                                Math.min(angleBonus, 125 / current.strainTime) *
                                // Scale buff from 300 BPM 1/2 to 400 BPM 1/2.
                                Math.pow(Math.sin(Math.PI / 2 *  Math.min(1, (100 - current.strainTime) / 25)), 2) *
                                // Buff distance exceeding 50 (radius) up to 100 (diameter).
                                Math.pow(Math.sin(Math.PI / 2 * (MathUtils.clamp(current.lazyJumpDistance, 50, 100) - 50) / 50), 2);
            }

            // Penalize wide angles if they're repeated, reducing the penalty as last.angle gets more acute.
            wideAngleBonus *= angleBonus * (1 - Math.min(wideAngleBonus, Math.pow(calculateWideAngleBonus(last.angle), 3)));
            // Penalize acute angles if they're repeated, reducing the penalty as lastLast.angle gets more obtuse.
            acuteAngleBonus *= 0.5 + 0.5 * (1 - Math.min(acuteAngleBonus, Math.pow(calculateAcuteAngleBonus(lastLast.angle), 3)));
        }

        if (Math.max(prevVelocity, currentVelocity) != 0) {
            // We want to use the average velocity over the whole object when awarding differences, not the individual jump and slider path velocities.
            prevVelocity = (last.lazyJumpDistance + lastLast.travelDistance) / last.strainTime;
            currentVelocity = (current.lazyJumpDistance + last.travelDistance) / current.strainTime;

            // Scale with ratio of difference compared to half the max distance.
            double distanceRatio = Math.pow(Math.sin(Math.PI / 2 * Math.abs(prevVelocity - currentVelocity) / Math.max(prevVelocity, currentVelocity)), 2);

            // Reward for % distance up to 125 / strainTime for overlaps where velocity is still changing.
            double overlapVelocityBuff = Math.min(125 / Math.min(current.strainTime, last.strainTime), Math.abs(prevVelocity - currentVelocity));

            velocityChangeBonus = overlapVelocityBuff * distanceRatio;

            // Penalize for rhythm changes.
            velocityChangeBonus *= Math.pow(Math.min(current.strainTime, last.strainTime) / Math.max(current.strainTime, last.strainTime), 2);
        }

        if (last.object instanceof Slider) {
            // Reward sliders based on velocity.
            sliderBonus = last.travelDistance / last.travelTime;
        }

        // Add in acute angle bonus or wide angle bonus + velocity change bonus, whichever is larger.
        strain += Math.max(acuteAngleBonus * acuteAngleMultiplier, wideAngleBonus * wideAngleMultiplier + velocityChangeBonus * velocityChangeMultiplier);

        // Add in additional slider velocity bonus.
        if (withSliders) {
            strain += sliderBonus * sliderMultiplier;
        }

        return strain;
    }

    /**
     * Calculates the bonus of wide angles.
     */
    private static double calculateWideAngleBonus(double angle) {
        return Math.pow(
                Math.sin(
                        (3.0 / 4) *
                                (Math.min((5.0 / 6) * Math.PI, Math.max(Math.PI / 6, angle)) -
                                        Math.PI / 6)
                ),
                2
        );
    }

    /**
     * Calculates the bonus of acute angles.
     */
    private static double calculateAcuteAngleBonus(double angle) {
        return 1 - calculateWideAngleBonus(angle);
    }
}
