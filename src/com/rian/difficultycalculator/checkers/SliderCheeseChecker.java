package com.rian.difficultycalculator.checkers;

import com.rian.difficultycalculator.attributes.DifficultSlider;
import com.rian.difficultycalculator.attributes.ExtendedRimuDifficultyAttributes;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHitObject;
import com.rian.difficultycalculator.math.Interpolation;
import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.utils.CircleSizeCalculator;
import com.rian.difficultycalculator.utils.GameMode;
import com.rian.difficultycalculator.utils.RimuHitWindowConverter;
import com.rian.difficultycalculator.utils.StandardHitWindowConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.scoring.Replay;
import ru.nsu.ccfit.zuev.osu.scoring.TouchType;

/**
 * Utility to check whether relevant sliders in a beatmap are cheesed.
 */
public final class SliderCheeseChecker {
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
     * The meh osu!droid hit window of the analyzed beatmap.
     */
    private final double mehWindow;

    private final double trueObjectScale;

    /**
     * @param beatmap The beatmap that is being analyzed.
     * @param difficultyAttributes The difficulty attributes of the beatmap.
     * @param cursorMoves The cursor moves that were done.
     * @param objectData Data regarding hit object information.
     */
    public SliderCheeseChecker(DifficultyBeatmap beatmap,
                               ExtendedRimuDifficultyAttributes difficultyAttributes,
                               List<Replay.MoveArray> cursorMoves,
                               Replay.ReplayObjectData[] objectData) {
        this.beatmap = beatmap;
        objects = beatmap.getHitObjectsManager().getObjects();
        this.difficultyAttributes = difficultyAttributes;
        this.cursorMoves = cursorMoves;
        this.objectData = objectData;

        double realODMS = StandardHitWindowConverter.odToHitWindow300(difficultyAttributes.overallDifficulty) / difficultyAttributes.clockRate;
        boolean isPrecise = difficultyAttributes.mods.contains(GameMod.MOD_PRECISE);
        double rimuOD = RimuHitWindowConverter.hitWindow300ToOD(realODMS, isPrecise);

        mehWindow = RimuHitWindowConverter.odToHitWindow50(rimuOD, isPrecise);
        trueObjectScale = CircleSizeCalculator.rimuCSToRimuScale(beatmap.getDifficultyManager().getCS(), difficultyAttributes.mods);
    }

    /**
     * Checks if relevant sliders in the given beatmap was cheesed.
     *
     * @return A structure containing information about the check.
     */
    public SliderCheeseInformation check() {
        if (difficultyAttributes.difficultSliders.isEmpty()) {
            return new SliderCheeseInformation(1, 1, 1);
        }

        ArrayList<Double> cheesedDifficultyRatings = obtainCheesedSlidersDifficultyRating();
        return calculatePenalty(cheesedDifficultyRatings);
    }

    private ArrayList<Double> obtainCheesedSlidersDifficultyRating() {
        // Current loop indices are stored for efficiency.
        int[] cursorLoopIndices = new int[cursorMoves.size()];
        Arrays.fill(cursorLoopIndices, 1);

        ArrayList<Double> cheesedDifficultyRatings = new ArrayList<>();
        double objectRadius = HitObject.OBJECT_RADIUS * trueObjectScale;
        double sliderBallRadius = objectRadius * 2;

        List<DifficultSlider> difficultSliders = difficultyAttributes.difficultSliders.subList(0, difficultyAttributes.difficultSliders.size() - 1);
        // Sort difficult sliders by index so that cursor loop indices work properly.
        difficultSliders.sort(Comparator.comparingInt(a -> a.index));

        for (DifficultSlider difficultSlider : difficultSliders) {
            if (difficultSlider.index >= objectData.length) {
                continue;
            }

            // If a slider break occurs, we disregard the check for that slider.
            if (objectData[difficultSlider.index].accuracy == Math.floor(mehWindow) + 13) {
                continue;
            }

            Slider object = (Slider) objects.get(difficultSlider.index);

            if (object.getRimuScale() != trueObjectScale) {
                // Deep clone the object to not modify it game-wide.
                object = object.deepClone();
                object.setRimuScale(trueObjectScale);
            }

            Vector2 objectStartPosition = object.getStackedPosition(GameMode.rimu);

            // These time boundaries should consider the delta time between the previous and next
            // object as well as their hit accuracy. However, they are somewhat complicated to
            // compute and the accuracy gain is small. As such, let's settle with 50 hit window.
            double minTimeLimit = object.getStartTime() - mehWindow;
            double maxTimeLimit = object.getStartTime() + mehWindow;

            // Get the closest tap distance across all cursors.
            double[] closestDistances = new double[cursorMoves.size()];
            int[] closestIndices = new int[cursorMoves.size()];

            for (int i = 0; i < cursorMoves.size(); ++i) {
                Replay.MoveArray cursorMove = cursorMoves.get(i);
                double closestDistance = Double.POSITIVE_INFINITY;
                int closestIndex = cursorMove.size;

                for (int j = cursorLoopIndices[i]; j < cursorMove.size; j = ++cursorLoopIndices[i]) {
                    Replay.ReplayMovement movement = cursorMove.movements[j];
                    Replay.ReplayMovement prevMovement = cursorMove.movements[j - 1];

                    if (prevMovement.getTime() < minTimeLimit) {
                        continue;
                    }

                    if (prevMovement.getTime() > maxTimeLimit) {
                        break;
                    }

                    if (prevMovement.getTouchType() == TouchType.DOWN && prevMovement.getTime() >= minTimeLimit) {
                        double distance = new Vector2(prevMovement.getPoint()).getDistance(objectStartPosition);

                        if (closestDistance > distance) {
                            closestDistance = distance;
                            closestIndex = j;
                        }

                        if (closestDistance <= objectRadius) {
                            break;
                        }
                    }

                    // Check if there are cursor presses within the cursor's active time.
                    for (int k = 0; k < cursorMoves.size(); ++k) {
                        // Skip the current cursor index.
                        if (k == i) {
                            continue;
                        }

                        for (Replay.ReplayMovement press : cursorMoves.get(k).movements) {
                            if (press.getTouchType() != TouchType.DOWN) {
                                continue;
                            }
                            if (press.getTime() < minTimeLimit) {
                                continue;
                            }
                            if (press.getTime() > maxTimeLimit) {
                                break;
                            }

                            double distance = Double.POSITIVE_INFINITY;

                            // We will not consider presses here as it has already been processed above.
                            switch (movement.getTouchType()) {
                                case MOVE:
                                    double t = (double) (press.getTime() - prevMovement.getTime()) / (movement.getTime() - prevMovement.getTime());
                                    Vector2 cursorPosition = new Vector2(
                                            Interpolation.linear(prevMovement.getPoint().x, movement.getPoint().x, t),
                                            Interpolation.linear(prevMovement.getPoint().y, movement.getPoint().y, t)
                                    );

                                    distance = cursorPosition.getDistance(objectStartPosition);
                                    break;
                                case UP:
                                    distance = new Vector2(prevMovement.getPoint()).getDistance(objectStartPosition);
                                    break;
                            }

                            if (closestDistance > distance) {
                                closestDistance = distance;
                                closestIndex = j;
                            }

                            if (closestDistance <= objectRadius) {
                                break;
                            }
                        }
                    }
                }

                closestDistances[i] = closestDistance;
                closestIndices[i] = closestIndex;

                if (cursorLoopIndices[i] > 0) {
                    // Decrement the index. The previous group may also have a role on the next slider.
                    --cursorLoopIndices[i];
                }
            }

            int index = 0;
            int movementIndex = closestIndices[index];
            double closestDistance = closestDistances[index];

            for (int i = 1; i < closestIndices.length; ++i) {
                if (closestDistances[i] <= closestDistance) {
                    closestDistance = closestDistances[i];
                    movementIndex = closestIndices[i];
                    index = i;
                }
            }

            if (closestDistance > objectRadius) {
                // Closest press is not in slider head. Abort.
                cheesedDifficultyRatings.add(difficultSlider.difficultyRating);
                continue;
            }

            boolean isCheesed = false;
            // Track cursor movement to see if it lands on every tick.
            Replay.MoveArray cursorMove = cursorMoves.get(index);

            for (int k = 1; k < object.getNestedHitObjects().size() && !isCheesed; ++k) {
                if (!objectData[difficultSlider.index].tickSet.get(k - 1)) {
                    continue;
                }

                SliderHitObject nestedObject = object.getNestedHitObjects().get(k);
                Vector2 nestedPosition = nestedObject.getStackedPosition(GameMode.rimu);

                while (movementIndex < cursorMove.size && cursorMove.movements[movementIndex].getTime() < nestedObject.getStartTime()) {
                    if (cursorMove.movements[movementIndex].getTouchType() == TouchType.UP) {
                        isCheesed = true;
                        break;
                    }

                    ++movementIndex;
                }

                if (movementIndex == cursorMove.size) {
                    break;
                }

                Replay.ReplayMovement currentMovement = cursorMove.movements[movementIndex];
                Replay.ReplayMovement prevMovement = cursorMove.movements[movementIndex - 1];

                switch (currentMovement.getTouchType()) {
                    case MOVE:
                        // Interpolate cursor position during nested object time.
                        double t = (nestedObject.getStartTime() - prevMovement.getTime()) / (currentMovement.getTime() - prevMovement.getTime());
                        Vector2 cursorPosition = new Vector2(
                                Interpolation.linear(prevMovement.getPoint().x, currentMovement.getPoint().x, t),
                                Interpolation.linear(prevMovement.getPoint().y, currentMovement.getPoint().y, t)
                        );

                        double distance = cursorPosition.getDistance(nestedPosition);
                        isCheesed = distance > sliderBallRadius;
                        break;
                    case UP:
                        isCheesed = new Vector2(prevMovement.getPoint()).getDistance(nestedPosition) > sliderBallRadius;
                        break;
                }
            }

            if (isCheesed) {
                cheesedDifficultyRatings.add(difficultSlider.difficultyRating);
            }
        }

        return cheesedDifficultyRatings;
    }

    private SliderCheeseInformation calculatePenalty(List<Double> cheesedDifficultyRatings) {
        double summedDifficultyRating = 0;
        for (double rating : cheesedDifficultyRatings) {
            summedDifficultyRating += rating;
        }

        return new SliderCheeseInformation(
                Math.max(
                        difficultyAttributes.aimSliderFactor,
                        Math.pow(1 - summedDifficultyRating * difficultyAttributes.aimSliderFactor, 2)
                ),
                Math.max(
                        difficultyAttributes.flashlightSliderFactor,
                        Math.pow(1 - summedDifficultyRating * difficultyAttributes.flashlightSliderFactor, 2)
                ),
                Math.max(
                        difficultyAttributes.visualSliderFactor,
                        Math.pow(1 - summedDifficultyRating * difficultyAttributes.visualSliderFactor, 2)
                )
        );
    }
}
