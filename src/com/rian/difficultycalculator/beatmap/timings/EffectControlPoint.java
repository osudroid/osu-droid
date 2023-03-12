package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a control point that applies an effect to a beatmap (e.g. kiai).
 */
public class EffectControlPoint extends ControlPoint {
    /**
     * Whether kiai is enabled at this control point.
     */
    public final boolean isKiai;

    /**
     * @param time   The time at which this control point takes effect, in milliseconds.
     * @param isKiai Whether kiai is enabled at this control point.
     */
    public EffectControlPoint(int time, boolean isKiai) {
        super(time);

        this.isKiai = isKiai;
    }

    @Override
    public boolean isRedundant(ControlPoint existing) {
        return existing instanceof EffectControlPoint && isKiai == ((EffectControlPoint) existing).isKiai;
    }
}
