package com.rian.difficultycalculator.evaluators;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.math.MathUtils;

/**
 * An evaluator for calculating osu!standard speed skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class SpeedEvaluator {
    private static final double singleSpacingThreshold = 125;
    private static final double minSpeedBonus = 75;

    private SpeedEvaluator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     * <ul>
     *     <li>time between pressing the previous and current object,</li>
     *     <li>distance between those objects,</li>
     *     <li>and how easily they can be cheesed.</li>
     * </ul>
     *
     * @param current The current object.
     * @param greatWindow The great hit window of the current object.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, double greatWindow) {
        if (current.object instanceof Spinner) {
            return 0;
        }

        DifficultyHitObject prev = current.previous(0);

        double strainTime = current.strainTime;
        double greatWindowFull = greatWindow * 2;

        // Nerf double-tappable doubles.
        DifficultyHitObject next = current.next(0);
        double doubletapness = 1;

        if (next != null) {
            double currentDeltaTime = Math.max(1, current.deltaTime);
            double nextDeltaTime = Math.max(1, next.deltaTime);
            double deltaDifference = Math.abs(nextDeltaTime - currentDeltaTime);
            double speedRatio = currentDeltaTime / Math.max(currentDeltaTime, deltaDifference);
            double windowRatio = Math.pow(Math.min(1, currentDeltaTime / greatWindowFull), 2);
            doubletapness = Math.pow(speedRatio, 1 - windowRatio);
        }

        // Cap deltatime to the OD 300 hitwindow.
        // 0.93 is derived from making sure 260 BPM 1/4 OD8 streams aren't nerfed harshly, whilst 0.92 limits the effect of the cap.
        strainTime /= MathUtils.clamp(strainTime / greatWindowFull / 0.93, 0.92, 1);

        double speedBonus = 1;
        if (strainTime < minSpeedBonus) {
            speedBonus += 0.75 * Math.pow((minSpeedBonus - strainTime) / 40, 2);
        }

        double travelDistance = prev != null ? prev.travelDistance : 0;
        double distance = Math.min(singleSpacingThreshold, travelDistance + current.minimumJumpDistance);

        return (speedBonus + speedBonus * Math.pow(distance / singleSpacingThreshold, 3.5)) * doubletapness / strainTime;
    }
}
