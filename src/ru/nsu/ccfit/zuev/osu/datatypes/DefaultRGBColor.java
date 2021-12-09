package ru.nsu.ccfit.zuev.osu.datatypes;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class DefaultRGBColor extends DefaultData<RGBColor> {
    private final String instanceDefaultHex = "#FFFFFF";

    public DefaultRGBColor(RGBColor defaultValue) {
        super(defaultValue);
    }

    public String instanceDefaultHex() {
        return instanceDefaultHex;
    }

    @Override
    protected RGBColor instanceDefaultValue() {
        return RGBColor.hex2Rgb(instanceDefaultHex);
    }
}
