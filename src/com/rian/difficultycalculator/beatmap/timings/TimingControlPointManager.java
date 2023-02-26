package com.rian.difficultycalculator.beatmap.timings;

/**
 * A manager for timing control points.
 */
public class TimingControlPointManager extends ControlPointManager<TimingControlPoint> {
    public TimingControlPointManager() {
        super(new TimingControlPoint(0, 1000));
    }

    @Override
    public TimingControlPoint controlPointAt(int time) {
        return binarySearchWithFallback(time);
    }
}
