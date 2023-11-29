package ru.nsu.ccfit.zuev.osu.datatypes;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class DefaultRGBColor extends DefaultData<RGBColor> {

    public DefaultRGBColor(RGBColor defaultValue) {
        super(defaultValue);
    }

    @Override
    protected RGBColor instanceDefaultValue() {
        String instanceDefaultHex = "#FFFFFF";
        return RGBColor.hex2Rgb(instanceDefaultHex);
    }

}
