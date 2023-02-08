package com.reco1l.utils;

// Reco1l math xd
public final class Maths {

    //--------------------------------------------------------------------------------------------//

    private Maths() {}

    //--------------------------------------------------------------------------------------------//
    // Percentage of a value

    public static int pct(int value, float percent) {
        return (int) (value * (percent / 100f));
    }

    public static float pct(float value, float percent) {
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
