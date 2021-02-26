package test.tpdifficulty.hitobject;

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

    private int value;

    HitObjectType(int value) {
        this.value = value;
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

    public int value() {
        return this.value;
    }
}
