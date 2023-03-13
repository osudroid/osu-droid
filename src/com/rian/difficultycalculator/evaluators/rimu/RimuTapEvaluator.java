package com.rian.difficultycalculator.evaluators.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.math.MathUtils;

/**
 * An evaluator for calculating rimu! tap skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class RimuTapEvaluator {
    // ~200 1/4 BPM streams
    private static final double minSpeedBonus = 75;

    private RimuTapEvaluator() {
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
     * @param considerCheesability Whether to consider cheesability.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, double greatWindow,
                                              boolean considerCheesability) {
        // Exclude overlapping objects that can be tapped at once.
        if (current.object instanceof Spinner || current.isOverlapping(false)) {
            return 0;
        }

        double strainTime = current.strainTime;
        double doubletapness = 1;

        if (considerCheesability) {
            double greatWindowFull = greatWindow * 2;

            // Nerf double-tappable doubles.
            DifficultyHitObject next = current.next(0);

            if (next != null) {
                double currentDeltaTime = Math.max(1, current.deltaTime);
                double nextDeltaTime = Math.max(1, next.deltaTime);
                double deltaDifference = Math.abs(
                        nextDeltaTime - currentDeltaTime
                );
                double speedRatio = currentDeltaTime / Math.max(currentDeltaTime, deltaDifference);
                double windowRatio = Math.pow(Math.min(1, currentDeltaTime / greatWindowFull), 2);
                doubletapness = Math.pow(speedRatio, 1 - windowRatio);
            }

            // Cap deltatime to the OD 300 hit window.
            // 0.58 is derived from making sure 260 BPM 1/4 OD5 streams aren't nerfed harshly, whilst 0.91 limits the effect of the cap.
            strainTime /= MathUtils.clamp(strainTime / greatWindowFull / 0.58, 0.91, 1);
        }

        double speedBonus = 1;

        if (strainTime < minSpeedBonus) {
            speedBonus += 0.75 * Math.pow((minSpeedBonus - strainTime) / 40, 2);
        }

        return speedBonus * doubletapness / strainTime;
    }
}
