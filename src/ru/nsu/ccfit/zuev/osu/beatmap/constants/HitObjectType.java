package ru.nsu.ccfit.zuev.osu.beatmap.constants;

/**
 * Created by Fuuko on 2015/5/29.
 */
public enum HitObjectType {
    Normal(1),
    Slider(2),
    NewCombo(4),
    NormalNewCombo(5),
    SliderNewCombo(6),
    Spinner(8);

    private final int value;

    HitObjectType(int value) {
        this.value = value;
    }

    public static HitObjectType valueOf(int value) {
        return switch (value) {
            case 1 -> Normal;
            case 2 -> Slider;
            case 4 -> NewCombo;
            case 5 -> NormalNewCombo;
            case 6 -> SliderNewCombo;
            default -> Spinner;
        };
    }

    public int value() {
        return this.value;
    }
}
