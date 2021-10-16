package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultRGBColor;

public class ColorSkinData extends SkinData<RGBColor> {
    public ColorSkinData(String tag, RGBColor color) {
        super(tag, new DefaultRGBColor(color));
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        String defaultHex = "";
        String hex = data.optString(getTag(), defaultHex);
        setCurrentValue(hex.equals(defaultHex)? getDefaultValue() : RGBColor.hex2Rgb(hex));
    }
}
