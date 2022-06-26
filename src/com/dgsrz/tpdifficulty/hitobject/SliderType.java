package com.dgsrz.tpdifficulty.hitobject;

/**
 * Created by Fuuko on 2015/5/29.
 */
public enum SliderType {
    Catmull,
    Bezier,
    Linear,
    PerfectCurve;

    public static SliderType parse(char value) {
        switch (value) {
            case 'C':
                return Catmull;
            case 'B':
                return Bezier;
            case 'L':
                return Linear;
            case 'P':
                return PerfectCurve;
            default:
                return Bezier;
        }
    }
}
