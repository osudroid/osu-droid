package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHead;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTail;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTick;
import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a slider.
 */
public class Slider extends HitObjectWithDuration {
    /**
     * The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     */
    private final int repeatCount;

    /**
     * The end position of this slider.
     */
    private final Vector2 endPosition;

    /**
     * The path of this slider.
     */
    private final SliderPath path;

    /**
     * The nested hit objects of the slider.
     * <br><br>
     * Consists of head circle (slider head), slider ticks, repeat points, and tail circle (slider end).
     */
    private final ArrayList<SliderHitObject> nestedHitObjects = new ArrayList<>();

    /**
     * The velocity of this slider.
     */
    private final double velocity;

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param x                      The X position of this slider relative to the play field.
     * @param y                      The Y position of this slider relative to the play field.
     * @param timingControlPoint     The timing control point this slider is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     * @param path                   The path of this slider.
     * @param tickRate               The tick rate of the beatmap containing this slider.
     * @param tickDistanceMultiplier The multiplier for calculating slider ticks.
     */
    public Slider(double startTime, double x, double y,
                  TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                  int repeatCount, SliderPath path, double sliderVelocity, int tickRate, double tickDistanceMultiplier) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint, repeatCount,
                path, sliderVelocity, tickRate, tickDistanceMultiplier);
    }

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param position               The position of the slider relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     * @param path                   The path of this slider.
     * @param sliderVelocity         The slider velocity of the beatmap containing this slider.
     * @param tickRate               The tick rate of the beatmap containing this slider.
     * @param tickDistanceMultiplier The multiplier for calculating slider ticks.
     */
    public Slider(double startTime, Vector2 position,
                  TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                  int repeatCount, SliderPath path, double sliderVelocity, int tickRate,
                  double tickDistanceMultiplier) {
        // Temporarily set end time to start time. It will be evaluated later.
        super(startTime, startTime, position, timingControlPoint, difficultyControlPoint);

        this.repeatCount = repeatCount;
        this.path = path;

        double scoringDistance = 100 * sliderVelocity * difficultyControlPoint.speedMultiplier;
        velocity = scoringDistance / timingControlPoint.msPerBeat;

        endTime = startTime + repeatCount * path.expectedDistance / velocity;
        endPosition = position.add(path.positionAt(repeatCount % 2));

        double spanDuration = getDuration() / repeatCount;

        nestedHitObjects.add(new SliderHead(startTime, position, timingControlPoint, difficultyControlPoint));

        // A very lenient maximum length of a slider for ticks to be generated.
        // This exists for edge cases such as /b/1573664 where the beatmap has been edited by the user, and should never be reached in normal usage.
        int maxLength = 100000;
        double length = Math.min(maxLength, path.expectedDistance);
        double tickDistance = Math.min(Math.max(scoringDistance * tickRate / tickDistanceMultiplier, 0), length);

        if (tickDistance != 0) {
            double minDistanceFromEnd = velocity * 10;

            for (int span = 0; span < repeatCount; ++span) {
                double spanStartTime = startTime + span * spanDuration;
                boolean reversed = span % 2 == 1;

                ArrayList<SliderTick> sliderTicks = new ArrayList<>();

                for (double d = tickDistance; d <= length; d += tickDistance) {
                    if (d >= length - minDistanceFromEnd) {
                        break;
                    }

                    // Always generate ticks from the start of the path rather than the span to ensure
                    // that ticks in repeat spans are positioned identically to those in non-repeat spans
                    double distanceProgress = d / length;
                    double timeProgress = reversed ? 1 - distanceProgress : distanceProgress;

                    Vector2 tickPosition = position.add(path.positionAt(distanceProgress));

                    sliderTicks.add(
                            new SliderTick(
                                    spanStartTime + timeProgress * spanDuration,
                                    tickPosition,
                                    timingControlPoint,
                                    difficultyControlPoint,
                                    span,
                                    spanStartTime
                            )
                    );
                }

                // For repeat spans, ticks are returned in reverse-StartTime order.
                if (reversed) {
                    Collections.reverse(sliderTicks);
                }

                nestedHitObjects.addAll(sliderTicks);
            }
        }

        nestedHitObjects.add(
                new SliderTail(
                        endTime,
                        endPosition,
                        timingControlPoint,
                        difficultyControlPoint,
                        repeatCount - 1,
                        startTime + (repeatCount - 1) * spanDuration
                )
        );
    }

    /**
     * Gets the repetition amount of this slider.
     * <br>
     * Note that 1 repetition means no repeats (1 loop).
     */
    public int getRepeatCount() {
        return repeatCount;
    }

    /**
     * Gets the path of this slider.
     */
    public SliderPath getPath() {
        return path;
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        for (SliderHitObject object : nestedHitObjects) {
            object.setScale(scale);
        }
    }

    /**
     * Gets the nested hit objects of this slider.
     */
    public List<SliderHitObject> getNestedHitObjects() {
        return Collections.unmodifiableList(nestedHitObjects);
    }

    /**
     * Gets the end position of this slider.
     */
    public Vector2 getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the velocity of this slider.
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * Gets the stacked end position of this slider.
     */
    public Vector2 getStackedEndPosition() {
        return evaluateStackedPosition(endPosition);
    }
}
