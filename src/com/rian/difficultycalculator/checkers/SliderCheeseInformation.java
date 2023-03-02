package com.rian.difficultycalculator.checkers;

import com.rian.difficultycalculator.math.MathUtils;

/**
 * A structure containing information about slider cheese checking operation.
 */
public final class SliderCheeseInformation {
    /**
     * The value used to penalize the aim performance value, from 0 to 1.
     */
    public final double aimPenalty;

    /**
     * The value used to penalize the flashlight performance value, from 0 to 1.
     */
    public final double flashlightPenalty;

    /**
     * The value used to penalize the visual performance value, from 0 to 1.
     */
    public final double visualPenalty;

    /**
     * @param aimPenalty The value used to penalize the aim performance value, from 0 to 1.
     * @param flashlightPenalty The value used to penalize the flashlight performance value, from 0 to 1.
     * @param visualPenalty The value used to penalize the visual performance value, from 0 to 1.
     */
    public SliderCheeseInformation(double aimPenalty, double flashlightPenalty, double visualPenalty) {
        this.aimPenalty = MathUtils.clamp(aimPenalty, 0, 1);
        this.flashlightPenalty = MathUtils.clamp(flashlightPenalty, 0, 1);
        this.visualPenalty = MathUtils.clamp(visualPenalty, 0, 1);
    }

    /**
     * Whether the aim performance value should be penalized.
     */
    public boolean isAimPenalized() {
        return aimPenalty < 1;
    }

    /**
     * Whether the flashlight performance value should be penalized.
     */
    public boolean isFlashlightPenalized() {
        return flashlightPenalty < 1;
    }

    /**
     * Whether the visual performance value should be penalized.
     */
    public boolean isVisualPenalized() {
        return visualPenalty < 1;
    }
}
