package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.anddev.andengine.entity.sprite.Sprite;
import org.json.JSONObject;

public class SkinLayout {

    public JSONObject property;

    public float width, height, xOffset, yOffset;

    public float scale = 1;

    @NonNull
    public static SkinLayout load(@NonNull JSONObject object) {
        SkinLayout layout = new SkinLayout();
        layout.property = object;
        layout.width = (float) object.optDouble("w", -1);
        layout.height = (float) object.optDouble("h", -1);
        layout.xOffset = (float) object.optDouble("x", 0);
        layout.yOffset = (float) object.optDouble("y", 0);
        layout.scale = (float) object.optDouble("scale", -1);
        return layout;
    }

    public void baseApply(@NonNull Sprite entity) {
        entity.setPosition(xOffset, yOffset);
        if (scale != -1) {
            entity.setScale(scale);
        }
        if (width != -1) {
            entity.setWidth(width);
        }
        if (height != -1) {
            entity.setHeight(height);
        }
    }
}
