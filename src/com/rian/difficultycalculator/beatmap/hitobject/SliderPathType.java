package com.rian.difficultycalculator.beatmap.hitobject;

/**
 * Types of slider paths.
 */
public enum SliderPathType {
    Catmull,
    Bezier,
    Linear,
    PerfectCurve;

    public static SliderPathType parse(char value) {
        switch (value) {
            case 'C':
                return Catmull;
            case 'L':
                return Linear;
            case 'P':
                return PerfectCurve;
            default:
                return Bezier;
        }
    }
}
