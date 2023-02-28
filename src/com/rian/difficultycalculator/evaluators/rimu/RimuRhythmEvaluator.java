package com.rian.difficultycalculator.evaluators.rimu;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.utils.StandardHitWindowConverter;

import java.util.ArrayList;

/**
 * An evaluator for calculating rimu! rhythm skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class RimuRhythmEvaluator {
    private RimuRhythmEvaluator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates a rhythm multiplier for the difficulty of the tap associated
     * with historic data of the current object.
     *
     * @param current The current object.
     * @param greatWindow The great hit window of the current object.
     */
    public static double evaluateDifficultyOf(DifficultyHitObject current, double greatWindow) {
        // Exclude overlapping objects that can be tapped at once.
        if (current.object instanceof Spinner || current.deltaTime < 5) {
            return 1;
        }

        int previousIslandSize = 0;
        double rhythmComplexitySum = 0;
        int islandSize = 1;

        // Store the ratio of the current start of an island to buff for tighter rhythms.
        double startRatio = 0;

        boolean firstDeltaSwitch = false;
        int rhythmStart = 0;

        int historicalNoteCount = Math.min(current.index, 32);

        // Exclude overlapping objects that can be tapped at once.
        ArrayList<DifficultyHitObject> validPrevious = new ArrayList<>();

        for (int i = 0; i < historicalNoteCount; ++i) {
            DifficultyHitObject object = current.previous(i);

            if (object == null) {
                break;
            }

            if (object.deltaTime >= 5) {
                validPrevious.add(object);
            }
        }

        // 5 seconds of calculateRhythmBonus max.
        int historyTimeMax = 5000;
        while (rhythmStart < validPrevious.size() - 2 && current.startTime - validPrevious.get(rhythmStart).startTime < historyTimeMax) {
            ++rhythmStart;
        }

        for (int i = rhythmStart; i > 0; --i) {
            // Scale note 0 to 1 from history to now.
            double currentHistoricalDecay = (historyTimeMax - (current.startTime - validPrevious.get(i - 1).startTime)) / historyTimeMax;

            // Either we're limited by time or limited by object count.
            currentHistoricalDecay = Math.min(currentHistoricalDecay, (double) (validPrevious.size() - i) / validPrevious.size());

            double currentDelta = validPrevious.get(i - 1).strainTime;
            double prevDelta = validPrevious.get(i).strainTime;
            double lastDelta = validPrevious.get(i + 1).strainTime;

            double currentRatio = 1 + 6 * Math.min(0.5, Math.pow(Math.sin(Math.PI / (Math.min(prevDelta, currentDelta) / Math.max(prevDelta, currentDelta))), 2));

            double windowPenalty = Math.min(1, Math.max(0, Math.abs(prevDelta - currentDelta) - greatWindow * 0.4) / (greatWindow * 0.4));

            double effectiveRatio = windowPenalty * currentRatio;

            if (firstDeltaSwitch) {
                if (prevDelta <= 1.25 * currentDelta && prevDelta * 1.25 >= currentDelta) {
                    // Island is still progressing, count size.
                    if (islandSize < 7) {
                        ++islandSize;
                    }
                } else {
                    if (validPrevious.get(i - 1).object instanceof Slider) {
                        // BPM change is into slider, this is easy acc window.
                        effectiveRatio /= 8;
                    }

                    if (validPrevious.get(i).object instanceof Slider) {
                        // BPM change was from a slider, this is typically easier than circle -> circle.
                        effectiveRatio /= 4;
                    }

                    if (previousIslandSize == islandSize) {
                        // Repeated island size (ex: triplet -> triplet).
                        effectiveRatio /= 4;
                    }

                    if (previousIslandSize % 2 == islandSize % 2) {
                        // Repeated island polarity (2 -> 4, 3 -> 5).
                        effectiveRatio /= 2;
                    }

                    if (lastDelta > prevDelta + 10 && prevDelta > currentDelta + 10) {
                        // Previous increase happened a note ago.
                        // Albeit this is a 1/1 -> 1/2-1/4 type of transition, we don't want to buff this.
                        effectiveRatio /= 8;
                    }

                    rhythmComplexitySum += Math.sqrt(effectiveRatio * startRatio) * currentHistoricalDecay * Math.sqrt(4 + islandSize) / 2 * Math.sqrt(4 + previousIslandSize) / 2;

                    startRatio = effectiveRatio;

                    previousIslandSize = islandSize;

                    if (prevDelta * 1.25 < currentDelta) {
                        // We're slowing down, stop counting.
                        // If we're speeding up, this stays as is and we keep counting island size.
                        firstDeltaSwitch = false;
                    }

                    islandSize = 1;
                }
            } else if (prevDelta > 1.25 * currentDelta) {
                // We want to be speeding up.
                // Begin counting island until we change speed again.
                firstDeltaSwitch = true;
                startRatio = effectiveRatio;
                islandSize = 1;
            }
        }

        // Nerf doubles that can be tapped at the same time to get Great hit results.
        DifficultyHitObject next = current.next(0);
        double doubletapness = 1;

        if (next != null) {
            double currentDeltaTime = Math.max(1, current.deltaTime);
            double nextDeltaTime = Math.max(1, next.deltaTime);
            double deltaDifference = Math.abs(
                    nextDeltaTime - currentDeltaTime
            );
            double speedRatio = currentDeltaTime / Math.max(currentDeltaTime, deltaDifference);
            double windowRatio = Math.pow(Math.min(1, currentDeltaTime / (greatWindow * 2)), 2);
            doubletapness = Math.pow(speedRatio, 1 - windowRatio);
        }

        return Math.sqrt(4 + rhythmComplexitySum * calculateRhythmMultiplier(greatWindow) * doubletapness / 2);
    }

    /**
     * Calculates the rhythm multiplier of a given hit window.
     *
     * @param greatWindow The great hit window.
     */
    private static double calculateRhythmMultiplier(double greatWindow) {
        double od = StandardHitWindowConverter.hitWindow300ToOD(greatWindow);
        double odScaling = Math.pow(od, 2) / 400;

        return 0.75 + (od >= 0 ? odScaling : -odScaling);
    }
}
