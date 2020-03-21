package com.edlplan.osu.support.timing.controlpoint;

public class EffectControlPoint extends ControlPoint {
    private boolean kiaiModeOn;

    private boolean omitFirstBarLine;

    public boolean isKiaiModeOn() {
        return kiaiModeOn;
    }

    public void setKiaiModeOn(boolean kiaiModeOn) {
        this.kiaiModeOn = kiaiModeOn;
    }

    public boolean isOmitFirstBarLine() {
        return omitFirstBarLine;
    }

    public void setOmitFirstBarLine(boolean omitFirstBarLine) {
        this.omitFirstBarLine = omitFirstBarLine;
    }
}
