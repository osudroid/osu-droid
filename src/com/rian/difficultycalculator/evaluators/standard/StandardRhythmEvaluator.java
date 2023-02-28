package com.rian.difficultycalculator.evaluators.standard;

import com.rian.difficultycalculator.beatmap.hitobject.DifficultyHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;

import java.util.ArrayList;

/**
 * An evaluator for calculating osu!standard rhythm skill.
 * <br><br>
 * This class should be considered an "evaluating" class and not persisted.
 */
public final class StandardRhythmEvaluator {
    private static final double rhythmMultiplier = 0.75;
    private StandardRhythmEvaluator() {
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

        // 5 seconds of calculateRhythmBonus max.
        int historyTimeMax = 5000;
        while (rhythmStart < historicalNoteCount - 2 && current.startTime - current.previous(rhythmStart).startTime < historyTimeMax) {
            ++rhythmStart;
        }

        for (int i = rhythmStart; i > 0; --i) {
            DifficultyHitObject currentObject = current.previous(i - 1);
            DifficultyHitObject prevObject = current.previous(i);
            DifficultyHitObject lastObject = current.previous(i + 1);

            // Scale note 0 to 1 from history to now.
            double currentHistoricalDecay = (historyTimeMax - (current.startTime - currentObject.startTime)) / historyTimeMax;

            // Either we're limited by time or limited by object count.
            currentHistoricalDecay = Math.min(currentHistoricalDecay, (double) (historicalNoteCount - i) / historicalNoteCount);

            double currentDelta = currentObject.strainTime;
            double prevDelta = prevObject.strainTime;
            double lastDelta = lastObject.strainTime;

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
                    if (currentObject.object instanceof Slider) {
                        // BPM change is into slider, this is easy acc window.
                        effectiveRatio /= 8;
                    }

                    if (prevObject.object instanceof Slider) {
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

        return Math.sqrt(4 + rhythmComplexitySum * rhythmMultiplier / 2);
    }
}
