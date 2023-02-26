package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

/**
 * Represents a spinner.
 */
public class Spinner extends HitObjectWithDuration {
    /**
     * @param startTime              The time at which this spinner starts, in milliseconds.
     * @param endTime                The time at which this spinner ends, in milliseconds.
     * @param timingControlPoint     The timing control point this spinner is under effect on.
     * @param difficultyControlPoint The difficulty control point this spinner is under effect on.
     */
    public Spinner(double startTime, double endTime, TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        super(startTime, endTime, new Vector2(256, 192), timingControlPoint, difficultyControlPoint);
    }
}
