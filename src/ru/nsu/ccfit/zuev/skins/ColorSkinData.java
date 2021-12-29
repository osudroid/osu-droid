package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultRGBColor;

public class ColorSkinData extends SkinData<RGBColor> {
    private final String defaultHex;
    private String currentHex;

    public ColorSkinData(String tag, String defaultHex) {
        super(tag, new DefaultRGBColor(new RGBColor(RGBColor.hex2Rgb(defaultHex))));
        this.defaultHex = defaultHex;
        this.currentHex = defaultHex;
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        String hex = data.optString(getTag());
        if (hex.isEmpty()) {
            currentHex = defaultHex;
            setCurrentValue(getDefaultValue());
        } else {
            currentHex = hex;
            setCurrentValue(RGBColor.hex2Rgb(hex));
        }
    }

    @Override
    public boolean currentIsDefault() {
        return currentHex.equals(defaultHex);
    }
}
