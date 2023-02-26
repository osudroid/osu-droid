package com.rian.difficultycalculator.timings;

/**
 * Represents a control point that changes a beatmap's BPM.
 */
public class TimingControlPoint extends ControlPoint {
    /**
     * The amount of milliseconds passed for each beat.
     */
    public final double msPerBeat;

    /**
     * @param time      The time at which this control point takes effect, in milliseconds.
     * @param msPerBeat The amount of milliseconds passed for each beat.
     */
    public TimingControlPoint(int time, double msPerBeat) {
        super(time);

        this.msPerBeat = msPerBeat;
    }

    /**
     * Gets the BPM of this control point.
     */
    public double getBPM() {
        return 60000 / msPerBeat;
    }
}
