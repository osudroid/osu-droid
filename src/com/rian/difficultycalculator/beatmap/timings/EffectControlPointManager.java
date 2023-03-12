package com.rian.difficultycalculator.beatmap.timings;

/**
 * A manager for effect control points.
 */
public class EffectControlPointManager extends ControlPointManager<EffectControlPoint> {
    public EffectControlPointManager() {
        super(new EffectControlPoint(0, false));
    }

    @Override
    public EffectControlPoint controlPointAt(int time) {
        return binarySearchWithFallback(time);
    }
}
