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
        return switch (value) {
            case 'C' -> Catmull;
            case 'L' -> Linear;
            case 'P' -> PerfectCurve;
            default -> Bezier;
        };
    }
}
