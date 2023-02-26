package com.rian.difficultycalculator.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.timings.TimingControlPoint;

/**
 * Represents a slider.
 */
public class Slider extends HitObjectWithDuration {
    /**
     * The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     */
    public final int repeatCount;

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param x                      The X position of this slider relative to the play field.
     * @param y                      The Y position of this slider relative to the play field.
     * @param timingControlPoint     The timing control point this slider is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     */
    public Slider(int startTime, int x, int y,
                  TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                  int repeatCount) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint, repeatCount);
    }

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param position               The position of the slider relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     */
    public Slider(int startTime, Vector2 position,
                  TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                  int repeatCount) {
        // Temporarily set end time to start time. It will be evaluated later.
        super(startTime, startTime, position, timingControlPoint, difficultyControlPoint);

        this.repeatCount = repeatCount;
    }
}
