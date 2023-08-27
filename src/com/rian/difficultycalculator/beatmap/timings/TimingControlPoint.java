package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a control point that changes a beatmap's BPM.
 */
public class TimingControlPoint extends ControlPoint {
    /**
     * The amount of milliseconds passed for each beat.
     */
    public final double msPerBeat;

    /**
     * The time signature at this control point.
     */
    public final int timeSignature;

    /**
     * @param time          The time at which this control point takes effect, in milliseconds.
     * @param msPerBeat     The amount of milliseconds passed for each beat.
     * @param timeSignature The time signature at this control point.
     */
    public TimingControlPoint(double time, double msPerBeat, int timeSignature) {
        super(time);

        this.msPerBeat = msPerBeat;
        this.timeSignature = timeSignature;
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private TimingControlPoint(TimingControlPoint source) {
        this(source.time, source.msPerBeat, source.timeSignature);
    }

    /**
     * Gets the BPM of this control point.
     */
    public double getBPM() {
        return 60000 / msPerBeat;
    }

    @Override
    public boolean isRedundant(ControlPoint existing) {
        // Timing points are never redundant as they can change the time signature.
        return false;
    }

    @Override
    public TimingControlPoint deepClone() {
        return new TimingControlPoint(this);
    }
}
