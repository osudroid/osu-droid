package com.reco1l.framework;

import androidx.annotation.FloatRange;

public final class Maths {

    //--------------------------------------------------------------------------------------------//

    private Maths() {}

    //--------------------------------------------------------------------------------------------//
    // Percentage of a value

    public static int pct(int value, @FloatRange(from = 0, to = 100) float percent) {
        return (int) (value * (percent / 100f));
    }

    public static float pct(float value, @FloatRange(from = 0, to = 100) float percent) {
        return value * (percent / 100f);
    }

    //--------------------------------------------------------------------------------------------//

    public static int half(int value) {
        return value / 2;
    }

    public static float half(float value) {
        return value / 2;
    }

    //--------------------------------------------------------------------------------------------//

    public static int quart(int value) {
        return value / 4;
    }

    public static float quart(float value) {
        return value / 4;
    }
}
