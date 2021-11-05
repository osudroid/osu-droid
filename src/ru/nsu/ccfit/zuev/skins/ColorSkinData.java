package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultRGBColor;

public class ColorSkinData extends SkinData<RGBColor> {
    public ColorSkinData(String tag, RGBColor defaultValue) {
        super(tag, new DefaultRGBColor(defaultValue));
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        String hex = data.optString(getTag());
        setCurrentValue(hex.isEmpty() ? getDefaultValue() : RGBColor.hex2Rgb(hex));
    }
}
