package com.rian.difficultycalculator.beatmap.timings;

/**
 * A manager for difficulty control points.
 */
public class DifficultyControlPointManager extends ControlPointManager<DifficultyControlPoint> {
    public DifficultyControlPointManager() {
        super(new DifficultyControlPoint(0, 1, true));
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private DifficultyControlPointManager(DifficultyControlPointManager source) {
        super(source.defaultControlPoint);

        for (DifficultyControlPoint point : source.controlPoints) {
            controlPoints.add(point.deepClone());
        }
    }

    @Override
    public DifficultyControlPoint controlPointAt(double time) {
        return binarySearchWithFallback(time);
    }

    @Override
    public DifficultyControlPointManager deepClone() {
        return new DifficultyControlPointManager(this);
    }
}
