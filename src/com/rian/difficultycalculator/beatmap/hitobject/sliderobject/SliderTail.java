package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

/**
 * Represents a slider tail.
 */
public class SliderTail extends SliderHitObject {
    /**
     * @param startTime              The time at which this slider tail starts, in milliseconds.
     * @param position               The position of the slider tail relative to the play field.
     * @param timingControlPoint     The timing control point this slider tail is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider tail is under effect on.
     * @param spanIndex              The index of the span at which this slider tail lies.
     * @param spanStartTime          The start time of the span at which this slider tail lies, in milliseconds.
     */
    public SliderTail(double startTime, Vector2 position, TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint, int spanIndex, double spanStartTime) {
        super(startTime, position, timingControlPoint, difficultyControlPoint, spanIndex, spanStartTime);
    }
}
