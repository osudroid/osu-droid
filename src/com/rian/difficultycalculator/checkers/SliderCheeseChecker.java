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
        // Current loop indexes are stored for efficiency.
        int[] cursorLoopIndexes = new int[cursorMoves.size()];

        ArrayList<Double> cheesedDifficultyRatings = new ArrayList<>();
        double objectRadius = HitObject.OBJECT_RADIUS * trueObjectScale;
        double sliderBallRadius = objectRadius * 2;

        for (DifficultSlider difficultSlider : difficultyAttributes.difficultSliders) {
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
            boolean isCheesed = false;

            for (int i = 0; i < cursorMoves.size() && !isCheesed; ++i) {
                Replay.MoveArray cursorMove = cursorMoves.get(i);
                boolean isPressingSlider = false;

                for (int j = cursorLoopIndexes[i]; j < cursorMove.size; j = ++cursorLoopIndexes[i]) {
                    Replay.ReplayMovement movement = cursorMove.movements[j];

                    if (movement.getTime() < object.getStartTime() - mehWindow) {
                        continue;
                    }

                    if (movement.getTime() > object.getStartTime() + mehWindow) {
                        break;
                    }

                    if (movement.getTouchType() == TouchType.DOWN) {
                        Vector2 movementPosition = new Vector2(movement.getPoint());
                        isPressingSlider = movementPosition.getDistance(objectStartPosition) < objectRadius;
                    }

                    if (!isPressingSlider) {
                        continue;
                    }

                    // Track cursor movement to see if it lands on every tick.
                    int movementIndex = j + 1;
                    boolean isSliderFulfilled = true;

                    for (int k = 1; k < object.getNestedHitObjects().size() && isSliderFulfilled; ++k) {
                        if (!objectData[difficultSlider.index].tickSet.get(k - 1)) {
                            continue;
                        }

                        SliderHitObject nestedObject = object.getNestedHitObjects().get(k);
                        Vector2 nestedPosition = nestedObject.getStackedPosition(GameMode.rimu);

                        while (movementIndex < cursorMove.size && cursorMove.movements[movementIndex].getTime() < nestedObject.getStartTime()) {
                            if (cursorMove.movements[movementIndex].getTouchType() == TouchType.UP) {
                                isSliderFulfilled = false;
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
                                double t = (nestedObject.getStartTime() - prevMovement.getTime()) / (movement.getTime() - prevMovement.getTime());
                                Vector2 cursorPosition = new Vector2(
                                        Interpolation.linear(prevMovement.getPoint().x, movement.getPoint().x, t),
                                        Interpolation.linear(prevMovement.getPoint().y, movement.getPoint().y, t)
                                );

                                double distance = cursorPosition.getDistance(nestedPosition);
                                isSliderFulfilled = distance <= sliderBallRadius;
                                break;
                            case UP:
                                isSliderFulfilled = new Vector2(prevMovement.getPoint()).getDistance(nestedPosition) <= sliderBallRadius;
                                break;
                        }
                    }

                    isCheesed = !isSliderFulfilled;
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
                1 - summedDifficultyRating * difficultyAttributes.aimSliderFactor,
                1 - summedDifficultyRating * difficultyAttributes.flashlightSliderFactor,
                1 - summedDifficultyRating * difficultyAttributes.visualSliderFactor
        );
    }
}
