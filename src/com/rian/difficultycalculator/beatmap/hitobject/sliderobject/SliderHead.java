package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

/**
 * Represents the head of a slider.
 */
public class SliderHead extends SliderHitObject {
    /**
     * @param startTime              The start time of this slider head, in milliseconds.
     * @param position               The position of this slider head relative to the play field.
     * @param timingControlPoint     The timing control point this slider head under effect on.
     * @param difficultyControlPoint The difficulty control point this slider head is under effect on.
     */
    public SliderHead(double startTime, Vector2 position,
                      TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        super(startTime, position, timingControlPoint, difficultyControlPoint, 0, startTime);
    }
}
