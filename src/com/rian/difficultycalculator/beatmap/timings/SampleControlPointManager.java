package com.rian.difficultycalculator.beatmap.timings;

import com.rian.difficultycalculator.beatmap.constants.SampleBank;

/**
 * A manager for sample control points.
 */
public class SampleControlPointManager extends ControlPointManager<SampleControlPoint> {
    public SampleControlPointManager() {
        super(new SampleControlPoint(0, SampleBank.normal, 100, 0));
    }

    @Override
    public SampleControlPoint controlPointAt(int time) {
        return binarySearchWithFallback(time);
    }
}
