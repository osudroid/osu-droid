package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.reco1l.framework.Color4;
import com.reco1l.framework.HexComposition;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.datatypes.DefaultColor4;

public class ColorSkinData extends SkinData<Color4> {
    private final String defaultHex;
    private String currentHex;

    public ColorSkinData(String tag, String defaultHex) {
        super(tag, new DefaultColor4(new Color4(defaultHex, HexComposition.RRGGBB)));
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
            currentHex = hex.trim();
            setCurrentValue(new Color4(hex, HexComposition.RRGGBB));
        }
    }

    @Override
    public boolean currentIsDefault() {
        return currentHex.equalsIgnoreCase(defaultHex);
    }
}
