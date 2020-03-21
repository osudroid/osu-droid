package com.edlplan.osu.support.timing.controlpoint;

import com.edlplan.framework.math.FMath;

public class DifficultyControlPoint extends ControlPoint {
    private double speedMultiplier;

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = FMath.clamp(speedMultiplier, 0.1, 10);
    }
}
