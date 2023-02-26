package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

/**
 * Represents a hit circle.
 */
public class HitCircle extends HitObject {
    /**
     * @param startTime              The start time of this hit circle, in milliseconds.
     * @param position               The position of this hit circle relative to the play field.
     * @param timingControlPoint     The timing control point this hit circle is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit circle is under effect on.
     */
    public HitCircle(double startTime, Vector2 position,
                     TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        super(startTime, position, timingControlPoint, difficultyControlPoint);
    }

    /**
     * @param startTime              The start time of this hit circle, in milliseconds.
     * @param x                      The X position of this hit circle relative to the play field.
     * @param y                      The Y position of this hit circle relative to the play field.
     * @param timingControlPoint     The timing control point this hit circle is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit circle is under effect on.
     */
    public HitCircle(double startTime, double x, double y,
                     TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        super(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint);
    }
}
