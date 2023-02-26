package com.rian.difficultycalculator.beatmap.timings;

/**
 * A manager for difficulty control points.
 */
public class DifficultyControlPointManager extends ControlPointManager<DifficultyControlPoint> {
    public DifficultyControlPointManager() {
        super(new DifficultyControlPoint(0, 1, true));
    }

    @Override
    public DifficultyControlPoint controlPointAt(int time) {
        return binarySearchWithFallback(time);
    }
}
