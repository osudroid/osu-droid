package ru.nsu.ccfit.zuev.osu.datatypes;

import com.reco1l.framework.Color4;
import com.reco1l.framework.HexComposition;

public class DefaultRGBColor extends DefaultData<Color4> {
    private final String instanceDefaultHex = "#FFFFFF";

    public DefaultRGBColor(Color4 defaultValue) {
        super(defaultValue);
    }

    public String instanceDefaultHex() {
        return instanceDefaultHex;
    }

    @Override
    protected Color4 instanceDefaultValue() {
        return new Color4(instanceDefaultHex, HexComposition.RRGGBB);
    }
}
