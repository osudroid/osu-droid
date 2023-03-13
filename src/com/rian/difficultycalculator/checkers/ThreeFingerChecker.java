package com.rian.difficultycalculator.checkers;

import com.rian.difficultycalculator.attributes.ExtendedRimuDifficultyAttributes;
import com.rian.difficultycalculator.attributes.HighStrainSection;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.beatmap.timings.BreakPeriod;
import com.rian.difficultycalculator.math.Interpolation;
import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.utils.CircleSizeCalculator;
import com.rian.difficultycalculator.utils.GameMode;
import com.rian.difficultycalculator.utils.RimuHitWindowConverter;
import com.rian.difficultycalculator.utils.StandardHitWindowConverter;

import java.util.ArrayList;
import java.util.List;

import main.osu.game.mods.GameMod;
import main.osu.scoring.Replay;
import main.osu.scoring.TouchType;

/**
 * Utility to check whether or not a beatmap is three-fingered.
 */
public final class ThreeFingerChecker {
    /**
     * The beatmap that is being analyzed.
     */
    public final DifficultyBeatmap beatmap;

    /**
     * The difficulty attributes of the beatmap.
     */
    public final ExtendedRimuDifficultyAttributes difficultyAttributes;

    /**
     * The cursor moves that were done.
     */
    public final List<Replay.MoveArray> cursorMoves;

    /**
     * Data regarding hit object information.
     */
    public final Replay.ReplayObjectData[] objectData;

    /**
     * The hit objects in the beatmap.
     */
    private final List<HitObject> objects;

    /**
     * The threshold for the amount of cursors that are assumed to be pressed
     * by a single finger.
     */
    private final int cursorDistancingCountThreshold = 10;

    /**
     * The amount of notes that has a tap strain exceeding the strain threshold.
     */
    private int strainNoteCount;

    /**
     * The ratio threshold between non-3 finger cursors and 3-finger cursors.
     * <br><br>
     * Increasing this number will increase detection accuracy, however
     * it also increases the chance of falsely flagged plays.
     */
    private final double threeFingerRatioThreshold = 0.01;

    /**
     * Extended sections of the beatmap for drag detection.
     */
    private final ArrayList<ThreeFingerBeatmapSection> beatmapSections = new ArrayList<>();

    /**
     * A reprocessed break points to match right on object time.
     * <br><br>
     * This is used to increase detection accuracy since break points do not start right at the
     * start of the hit object before it and do not end right at the first hit object after it.
     */
    private final ArrayList<BreakPeriod> accurateBreakPeriods = new ArrayList<>();

    /**
     * A cursor occurrence nested array that only contains `movementType.DOWN` movement ID occurrences.
     *
     * Each index represents the cursor index.
     */
    private final ArrayList<ArrayList<Replay.ReplayMovement>> downCursorMoves;

    private final double greatWindow;
    private final double okWindow;
    private final double mehWindow;
    private final double trueObjectScale;

    /**
     * @param beatmap The beatmap that is being analyzed.
     * @param difficultyAttributes The difficulty attributes of the beatmap.
     * @param cursorMoves The cursor moves that were done.
     * @param objectData Data regarding hit object information.
     */
    public ThreeFingerChecker(DifficultyBeatmap beatmap,
                              ExtendedRimuDifficultyAttributes difficultyAttributes,
                              List<Replay.MoveArray> cursorMoves,
                              Replay.ReplayObjectData[] objectData) {
        this.beatmap = beatmap;
        objects = beatmap.getHitObjectsManager().getObjects();
        this.difficultyAttributes = difficultyAttributes;
        this.cursorMoves = cursorMoves;
        this.objectData = objectData;

        for (HighStrainSection section : difficultyAttributes.possibleThreeFingeredSections) {
            strainNoteCount += section.lastObjectIndex - section.firstObjectIndex + 1;
        }

        downCursorMoves = new ArrayList<>();
        for (int i = 0; i < cursorMoves.size(); ++i) {
            downCursorMoves.add(new ArrayList<>());
        }

        double realODMS = StandardHitWindowConverter.odToHitWindow300(difficultyAttributes.overallDifficulty) / difficultyAttributes.clockRate;
        boolean isPrecise = difficultyAttributes.mods.contains(GameMod.MOD_PRECISE);
        double rimuOD = RimuHitWindowConverter.hitWindow300ToOD(realODMS, isPrecise);

        greatWindow = RimuHitWindowConverter.odToHitWindow300(rimuOD, isPrecise);
        okWindow = RimuHitWindowConverter.odToHitWindow100(rimuOD, isPrecise);
        mehWindow = RimuHitWindowConverter.odToHitWindow50(rimuOD, isPrecise);

        trueObjectScale = CircleSizeCalculator.rimuCSToRimuScale(beatmap.getDifficultyManager().getCS(), difficultyAttributes.mods);
    }

    /**
     * Calculates the three-finger penalty of this score.
     * <br><br>
     * The beatmap will be separated into sections and each section will be determined
     * whether or not it is dragged.
     * <br><br>
     * After that, each section will be assigned a nerf factor based on whether or not
     * the section is 3-fingered. These nerf factors will be summed up into a final
     * nerf factor, taking beatmap difficulty into account.
     */
    public double calculatePenalty() {
        if (strainNoteCount == 0) {
            return 1;
        }

        getAccurateBreakPeriods();
        filterCursorInstances();

        if (downCursorMoves.stream().filter(r -> !r.isEmpty()).count() <= 3) {
            return 1;
        }

        getBeatmapSections();
        detectDragSections();
        preventAccidentalTaps();
        return calculateFinalPenalty();
    }

    /**
     * Generates a new set of "accurate break periods".
     *
     * This is done to increase detection accuracy since break periods do not start right at the
     * start of the hit object before it and do not end right at the first hit object after it.
     */
    private void getAccurateBreakPeriods() {
        accurateBreakPeriods.clear();

        for (BreakPeriod breakPeriod : beatmap.breakPeriods) {
            int beforeIndex = 0;
            while (beforeIndex < objects.size() - 1) {
                HitObject object = objects.get(beforeIndex);

                double endTime = object.getStartTime();
                if (object instanceof HitObjectWithDuration) {
                    endTime = ((HitObjectWithDuration) object).getEndTime();
                }

                if (endTime >= breakPeriod.startTime) {
                    break;
                }

                ++beforeIndex;
            }

            --beforeIndex;
            HitObject objectBefore = objects.get(beforeIndex);
            double timeBefore = objectBefore.getStartTime();

            if (objectBefore instanceof HitObjectWithDuration) {
                timeBefore = ((HitObjectWithDuration) objectBefore).getEndTime();
            }

            double beforeIndexHitWindowLength = mehWindow;
            switch (objectData[beforeIndex].result) {
                case 4:
                    beforeIndexHitWindowLength = greatWindow;
                    break;
                case 3:
                    beforeIndexHitWindowLength = okWindow;
                    break;
            }

            timeBefore += beforeIndexHitWindowLength;

            int afterIndex = beforeIndex + 1;
            double timeAfter = objects.get(afterIndex).getStartTime();

            double afterIndexHitWindowLength = mehWindow;
            switch (objectData[afterIndex].result) {
                case 4:
                    afterIndexHitWindowLength = greatWindow;
                    break;
                case 3:
                    afterIndexHitWindowLength = okWindow;
                    break;
            }

            timeAfter += afterIndexHitWindowLength;

            accurateBreakPeriods.add(new BreakPeriod(timeBefore, timeAfter));
        }
    }

    /**
     * Filters the original cursor instances, returning only those with <code>TouchType.DOWN</code> movement ID.
     * <br><br>
     * This also filters cursors that are in break period or happen before start/after end of the beatmap.
     */
    private void filterCursorInstances() {
        // For sliders and spinners, automatically set hit window length to be as lenient as possible.
        double firstObjectHitWindow = mehWindow;
        if (objects.get(0) instanceof HitCircle) {
            switch (objectData[0].result) {
                case 4:
                    firstObjectHitWindow = greatWindow;
                    break;
                case 3:
                    firstObjectHitWindow = okWindow;
                    break;
            }
        }

        // For sliders and spinners, automatically set hit window length to be as lenient as possible.
        double lastObjectHitWindow = mehWindow;
        if (objects.get(objects.size() - 1) instanceof HitCircle) {
            switch (objectData[objectData.length - 1].result) {
                case 4:
                    lastObjectHitWindow = greatWindow;
                    break;
                case 3:
                    lastObjectHitWindow = okWindow;
                    break;
            }
        }

        // These hit time uses hit window length as threshold.
        // This is because cursors aren't recorded exactly at hit time.
        double firstObjectHitTime = objects.get(0).getStartTime() - firstObjectHitWindow;
        double lastObjectHitTime = objects.get(objects.size() - 1).getStartTime() + lastObjectHitWindow;

        for (int i = 0; i < cursorMoves.size(); ++i) {
            ArrayList<Replay.ReplayMovement> movements = downCursorMoves.get(i);
            movements.clear();
            Replay.MoveArray originalMovements = cursorMoves.get(i);

            for (int j = 0; j < originalMovements.size; ++j) {
                Replay.ReplayMovement movement = originalMovements.movements[j];

                if (movement.getTouchType() != TouchType.DOWN) {
                    continue;
                }

                if (movement.getTime() < firstObjectHitTime) {
                    continue;
                }

                if (movement.getTime() > lastObjectHitTime) {
                    break;
                }

                if (accurateBreakPeriods.stream().anyMatch(b -> movement.getTime() >= b.startTime && movement.getTime() <= b.endTime)) {
                    continue;
                }

                movements.add(movement);
            }
        }
    }

    /**
     * Divides the beatmap into sections, which will be used to detect dragged sections and improve detection speed.
     */
    private void getBeatmapSections() {
        beatmapSections.clear();

        for (HighStrainSection section : difficultyAttributes.possibleThreeFingeredSections) {
            beatmapSections.add(new ThreeFingerBeatmapSection(section));
        }
    }

    /**
     * Checks whether each beatmap section is dragged.
     */
    private void detectDragSections() {
        for (ThreeFingerBeatmapSection section : beatmapSections) {
            int dragIndex = checkDrag(section);

            section.isDragged = dragIndex != -1;
            section.dragFingerIndex = dragIndex;
        }
    }
    /**
     * Checks if a section is dragged and returns the index of the drag finger.
     * <br><br>
     * If the section is not dragged, -1 will be returned.
     *
     * @param section The section to check.
     */
    private int checkDrag(ThreeFingerBeatmapSection section) {
        HitObject firstObject = objects.get(section.firstObjectIndex);
        HitObject lastObject = objects.get(section.lastObjectIndex);

        double firstObjectHitWindow = mehWindow;
        if (firstObject instanceof HitCircle) {
            switch (objectData[section.firstObjectIndex].result) {
                case 4:
                    firstObjectHitWindow = greatWindow;
                    break;
                case 3:
                    firstObjectHitWindow = okWindow;
                    break;
            }
        }

        double lastObjectHitWindow = mehWindow;
        if (lastObject instanceof HitCircle) {
            switch (objectData[section.lastObjectIndex].result) {
                case 4:
                    lastObjectHitWindow = greatWindow;
                    break;
                case 3:
                    lastObjectHitWindow = okWindow;
                    break;
            }
        }

        double firstObjectMinHitTime = firstObject.getStartTime() - firstObjectHitWindow;
        double lastObjectMaxHitTime = lastObject.getStartTime() + lastObjectHitWindow;

        // Since there may be more than 1 cursor move index,
        // we check which cursor move follows hit objects all over.
        ArrayList<Integer> cursorIndexes = new ArrayList<>();
        for (int i = 0; i < cursorMoves.size(); ++i) {
            Replay.MoveArray cursorMove = cursorMoves.get(i);

            for (int j = 0; j < cursorMove.size; ++j) {
                Replay.ReplayMovement movement = cursorMove.movements[j];

                if (movement.getTouchType() == TouchType.MOVE && movement.getTime() >= firstObjectMinHitTime && movement.getTime() <= lastObjectMaxHitTime) {
                    cursorIndexes.add(i);
                    break;
                }
            }
        }

        if (cursorIndexes.isEmpty()) {
            return -1;
        }

        for (int i = section.firstObjectIndex; i <= section.lastObjectIndex && cursorIndexes.stream().allMatch(a -> a == -1); ++i) {
            HitObject object = objects.get(i);
            Replay.ReplayObjectData data = objectData[i];

            if (object instanceof Spinner || data.result == 1) {
                continue;
            }

            // Exclude slider breaks.
            if (object instanceof Slider && data.accuracy == Math.floor(mehWindow) + 13) {
                continue;
            }

            if (object.getRimuScale() != trueObjectScale) {
                // Deep clone the object to not modify it game-wide.
                object = object.deepClone();
                object.setRimuScale(trueObjectScale);
            }

            Vector2 objectPosition = object.getStackedPosition(GameMode.rimu);
            double objectRadius = object.getRadius(GameMode.rimu);
            double hitTime = object.getStartTime() + data.accuracy;

            // Observe the cursor position at the object's hit time.
            for (int j = 0; j < cursorIndexes.size(); ++j) {
                if (cursorIndexes.get(j) == -1) {
                    continue;
                }

                Replay.MoveArray cursorMove = cursorMoves.get(cursorIndexes.get(j));
                int nextHitIndex = 0;
                while (nextHitIndex < cursorMove.size && cursorMove.movements[nextHitIndex].getTime() < hitTime) {
                    ++nextHitIndex;
                }
                int hitIndex = nextHitIndex - 1;

                if (nextHitIndex == cursorMove.size) {
                    cursorIndexes.set(j, -1);
                    continue;
                }
                if (cursorMove.movements[hitIndex].getTouchType() == TouchType.UP) {
                    cursorIndexes.set(j, -1);
                    continue;
                }

                Replay.ReplayMovement movement = cursorMove.movements[hitIndex];
                Replay.ReplayMovement nextMovement = cursorMove.movements[nextHitIndex];
                Vector2 cursorPosition = new Vector2(movement.getPoint());
                boolean isInObject = false;

                if (nextMovement.getTouchType() == TouchType.MOVE) {
                    for (int mSecPassed = movement.getTime(); mSecPassed <= nextMovement.getTime(); ++mSecPassed) {
                        double t = (double) (mSecPassed - movement.getTime()) / (nextMovement.getTime() - movement.getTime());

                        Vector2 currentCursorPosition = new Vector2(
                                Interpolation.linear(cursorPosition.x, nextMovement.getPoint().x, t),
                                Interpolation.linear(cursorPosition.y, nextMovement.getPoint().y, t)
                        );

                        if (objectPosition.getDistance(currentCursorPosition) <= objectRadius) {
                            isInObject = true;
                            break;
                        }
                    }
                } else {
                    isInObject = objectPosition.getDistance(cursorPosition) <= objectRadius;
                }

                if (!isInObject) {
                    cursorIndexes.set(j, -1);
                }
            }

            cursorIndexes.removeIf(p -> p == -1);
        }

        return cursorIndexes.isEmpty() ? -1 : cursorIndexes.get(0);
    }

    /**
     * Attempts to prevent accidental taps from being flagged.
     * <br><br>
     * This detection will filter cursors that don't hit
     * any object in beatmap sections, thus eliminating any
     * unnecessary taps.
     */
    private void preventAccidentalTaps() {
        int filledCursorAmount = (int) downCursorMoves.stream().filter(m -> m.size() > 0).count();

        // This threshold is used to filter out accidental taps.
        //
        // Increasing this number makes the filtration more sensitive, however it
        // will also increase the chance of 3-fingered plays getting out from
        // being flagged.
        final double accidentalTapThreshold = 400;

        List<HitObject> objects = beatmap.getHitObjectsManager().getObjects();

        int totalCursorAmount = 0;
        for (ArrayList<Replay.ReplayMovement> cursorMove : downCursorMoves) {
            totalCursorAmount += cursorMove.size();
        }

        for (int i = 0; i < downCursorMoves.size() && filledCursorAmount > 3; ++i) {
            ArrayList<Replay.ReplayMovement> cursorMove = downCursorMoves.get(i);

            // Use an estimation for accidental tap threshold.
            if (cursorMove.size() <= Math.ceil(objects.size() / accidentalTapThreshold) &&
                    (double) cursorMove.size() / totalCursorAmount < threeFingerRatioThreshold * 2) {
                --filledCursorAmount;
                cursorMove.clear();
            }
        }
    }

    /**
     * Calculates the final penalty.
     */
    private double calculateFinalPenalty() {
        ArrayList<Double> strainFactors = new ArrayList<>();
        ArrayList<Double> fingerFactors = new ArrayList<>();
        ArrayList<Double> lengthFactors = new ArrayList<>();

        // The distance threshold between cursors to assume that two cursors are
        // actually pressed with 1 finger in osu!pixels.
        //
        // This is used to prevent cases where a player would lift their finger
        // too fast to the point where the 4th cursor move or beyond is recorded
        // as 1st, 2nd, or 3rd cursor move.
        final double cursorDistancingDistanceThreshold = 60;

        // The threshold for the time difference of cursors that are assumed to be pressed
        // by a single finger, in milliseconds.
        final double cursorDistancingTimeThreshold = 1000;

        // We only filter cursor instances that are above the strain threshold.
        // This minimizes the amount of cursor instances to analyze.
        for (ThreeFingerBeatmapSection section : beatmapSections) {
            HitObject firstObject = objects.get(section.firstObjectIndex);
            double startTime = firstObject.getStartTime();
            Replay.ReplayObjectData startObjectData = objectData[section.firstObjectIndex];

            if (startObjectData.result == 1 || (firstObject instanceof Slider && startObjectData.accuracy == Math.floor(mehWindow) + 13)) {
                startTime -= mehWindow;
            } else {
                switch (startObjectData.result) {
                    case 4:
                        startTime -= greatWindow;
                        break;
                    case 3:
                        startTime -= okWindow;
                        break;
                    default:
                        startTime -= mehWindow;
                }
            }

            HitObject lastObject = objects.get(section.lastObjectIndex);
            double endTime = lastObject.getStartTime();
            if (!(lastObject instanceof HitCircle)) {
                endTime = ((HitObjectWithDuration) lastObject).getEndTime();
            }

            if (lastObject instanceof HitCircle) {
                switch (objectData[section.lastObjectIndex].result) {
                    case 4:
                        endTime += greatWindow;
                        break;
                    case 3:
                        endTime += okWindow;
                        break;
                    default:
                        endTime += mehWindow;
                }
            }

            ArrayList<Integer> cursorAmounts = new ArrayList<>();
            ArrayList<Vector2> cursorVectors = new ArrayList<>();
            ArrayList<Integer> cursorTimes = new ArrayList<>();

            for (int i = 0; i < downCursorMoves.size(); ++i) {
                // Do not include drag cursor move.
                if (i == section.dragFingerIndex) {
                    continue;
                }

                int amount = 0;
                for (Replay.ReplayMovement press : downCursorMoves.get(i)) {
                    if (press.getTime() >= startTime && press.getTime() <= endTime) {
                        ++amount;
                        cursorVectors.add(new Vector2(press.getPoint()));
                        cursorTimes.add(press.getTime());
                    }
                }

                cursorAmounts.add(amount);
            }

            // This index will be used to detect if a section is 3-fingered.
            // If the section is dragged, the dragged move will be ignored,
            // hence why the index is 1 less than non-dragged section.
            int threeFingerStartIndex = section.dragFingerIndex != -1 ? 2 : 3;

            int threeFingerAmount = 0;
            int nonThreeFingerAmount = 0;

            for (int i = 0; i < cursorAmounts.size(); ++i) {
                if (i >= threeFingerStartIndex) {
                    threeFingerAmount += cursorAmounts.get(i);
                } else {
                    nonThreeFingerAmount += cursorAmounts.get(i);
                }
            }

            // Divide >=4th (3rd for drag) cursor instances with 1st + 2nd (+ 3rd for non-drag)
            // to check if the section is 3-fingered.
            double threeFingerRatio = (double) nonThreeFingerAmount / threeFingerAmount;

            ArrayList<Vector2> similarPressVectors = new ArrayList<>();
            ArrayList<Integer> similarPressCounts = new ArrayList<>();
            ArrayList<Integer> similarPressLastTimes = new ArrayList<>();

            for (int i = 0; i < cursorVectors.size(); ++i) {
                Vector2 cursorVector = cursorVectors.get(i);
                int cursorTime = cursorTimes.get(i);

                int pressIndex = 0;
                while (pressIndex < similarPressVectors.size() &&
                        similarPressVectors.get(pressIndex).getDistance(cursorVector) > cursorDistancingDistanceThreshold) {
                    ++pressIndex;
                }

                if (pressIndex < similarPressVectors.size()) {
                    if (cursorTime - similarPressLastTimes.get(pressIndex) >= cursorDistancingTimeThreshold) {
                        similarPressVectors.remove(pressIndex);
                        similarPressCounts.remove(pressIndex);
                        similarPressLastTimes.remove(pressIndex);

                        similarPressVectors.add(cursorVector);
                        similarPressCounts.add(1);
                        similarPressLastTimes.add(cursorTime);

                        continue;
                    }

                    similarPressVectors.set(pressIndex, cursorVector);
                    similarPressLastTimes.set(pressIndex, cursorTime);
                    similarPressCounts.set(pressIndex, similarPressCounts.get(pressIndex) + 1);
                } else {
                    similarPressVectors.add(cursorVector);
                    similarPressLastTimes.add(cursorTime);
                    similarPressCounts.add(1);
                }
            }

            // Sort by highest count; assume the order is 3rd, 4th, 5th, ... finger
            similarPressCounts.removeIf(c -> c >= cursorDistancingCountThreshold);
            similarPressCounts.sort((i1, i2) -> i2 - i1);

            for (int i = 0; i < 2; ++i) {
                if (similarPressCounts.isEmpty()) {
                    break;
                }

                similarPressCounts.remove(0);
            }

            // Ignore cursor presses that are only 1 for now since they are very likely to be accidental
            if ((threeFingerRatio > threeFingerRatioThreshold && cursorAmounts.stream().filter(c -> c > 1).count() > threeFingerStartIndex) || similarPressCounts.size() > 0) {
                // We can ignore the first 3 (2 for drag) filled cursor instances
                // since they are guaranteed not 3 finger.
                cursorAmounts.subList(0, threeFingerStartIndex).clear();

                int objectCount = section.lastObjectIndex - section.firstObjectIndex + 1;
                // Finger factor applies more penalty if more fingers were used.
                double fingerFactor = 1;

                if (threeFingerRatio > threeFingerRatioThreshold) {
                    for (int i = 0; i < cursorAmounts.size(); ++i) {
                        fingerFactor += Math.pow((double) (i + 1) * cursorAmounts.get(i) * objectCount / strainNoteCount, 0.9);
                    }
                } else {
                    for (int i = 0; i < similarPressCounts.size(); ++i) {
                        fingerFactor += Math.pow((double) (i + 1) * similarPressCounts.get(i) / (cursorDistancingCountThreshold * 2) * objectCount / strainNoteCount, 0.2);
                    }
                }

                // Length factor applies more penalty if there are more 3-fingered object.
                double lengthFactor = 1 + Math.pow((double) objectCount / strainNoteCount, 1.2);

                strainFactors.add(Math.max(1, section.sumStrain));
                fingerFactors.add(fingerFactor);
                lengthFactors.add(lengthFactor);
            }
        }

        double finalPenalty = 1;

        for (int i = 0; i < strainFactors.size(); ++i) {
            finalPenalty += 0.015 * Math.pow(strainFactors.get(i) * fingerFactors.get(i) * lengthFactors.get(i), 1.05);
        }

        return finalPenalty;
    }
}
