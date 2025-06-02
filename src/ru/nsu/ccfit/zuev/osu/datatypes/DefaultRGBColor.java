package ru.nsu.ccfit.zuev.osu.datatypes;

import com.reco1l.framework.ColorARGB;
import com.reco1l.framework.HexComposition;

public class DefaultRGBColor extends DefaultData<ColorARGB> {
    private final String instanceDefaultHex = "#FFFFFF";

    public DefaultRGBColor(ColorARGB defaultValue) {
        super(defaultValue);
    }

    public String instanceDefaultHex() {
        return instanceDefaultHex;
    }

    @Override
    protected ColorARGB instanceDefaultValue() {
        return new ColorARGB(instanceDefaultHex, HexComposition.RRGGBB);
    }
}
