package ru.nsu.ccfit.zuev.osu.beatmap.constants;

/**
 * Created by Fuuko on 2015/5/29.
 */
public enum HitObjectType {
    Normal(), Slider(), NewCombo(), NormalNewCombo(), SliderNewCombo(), Spinner();

    HitObjectType() {
    }

    public static HitObjectType valueOf(int value) {
        switch (value) {
            case 1:
                return Normal;
            case 2:
                return Slider;
            case 4:
                return NewCombo;
            case 5:
                return NormalNewCombo;
            case 6:
                return SliderNewCombo;
            default:
                return Spinner;
        }
    }

}
