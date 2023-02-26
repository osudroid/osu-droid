package com.rian.difficultycalculator.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.timings.TimingControlPoint;

/**
 * Represents a slider tick.
 */
public class SliderTick extends SliderHitObject {
    /**
     * @param startTime              The time at which this slider tick starts, in milliseconds.
     * @param x                      The X position of the slider tick relative to the play field.
     * @param y                      The Y position of the slider tick relative to the play field.
     * @param timingControlPoint     The timing control point this slider tick is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider tick is under effect on.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderTick(double startTime, double x, double y,
                      TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                      int spanIndex, double spanStartTime) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint, spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this slider tick starts, in milliseconds.
     * @param position               The position of the slider tick relative to the play field.
     * @param timingControlPoint     The timing control point this slider tick is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider tick is under effect on.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderTick(double startTime, Vector2 position,
                      TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                      int spanIndex, double spanStartTime) {
        super(startTime, position, timingControlPoint, difficultyControlPoint, spanIndex, spanStartTime);
    }
}
