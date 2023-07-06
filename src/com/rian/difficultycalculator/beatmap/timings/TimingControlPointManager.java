package com.rian.difficultycalculator.beatmap.timings;

/**
 * A manager for timing control points.
 */
public class TimingControlPointManager extends ControlPointManager<TimingControlPoint> {
    public TimingControlPointManager() {
        super(new TimingControlPoint(0, 1000, 4));
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private TimingControlPointManager(TimingControlPointManager source) {
        super(source.defaultControlPoint);

        for (TimingControlPoint point : source.controlPoints) {
            controlPoints.add(point.deepClone());
        }
    }

    @Override
    public TimingControlPoint controlPointAt(double time) {
        return binarySearchWithFallback(time, controlPoints.size() > 0 ? controlPoints.get(0) : defaultControlPoint);
    }

    @Override
    public TimingControlPointManager deepClone() {
        return new TimingControlPointManager(this);
    }
}
