package com.edlplan.osu.support.timing.controlpoint;

import com.edlplan.framework.math.FMath;

public class TimingControlPoint extends ControlPoint {
    private int meter;

    private double beatLength;

    public int getMeter() {
        return meter;
    }

    public void setMeter(int meter) {
        this.meter = meter;
    }

    public double getBeatLength() {
        return beatLength;
    }

    public void setBeatLength(double beatLength) {
        this.beatLength = FMath.clamp(beatLength, 6, 6000);
    }
}
