package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.reco1l.andengine.sprite.ExtendedSprite;
import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.Config;

public class SkinLayout {

    public JSONObject property;

    public float width, height, xPosition, yPosition;

    public float scale = 1;

    @NonNull
    public static SkinLayout load(@NonNull JSONObject object) {
        SkinLayout layout = new SkinLayout();
        layout.property = object;
        layout.width = (float) object.optDouble("w", -1);
        layout.height = (float) object.optDouble("h", -1);
        layout.xPosition = (float) object.optDouble("x");
        layout.yPosition = (float) object.optDouble("y");
        layout.scale = (float) object.optDouble("scale", -1);
        return layout;
    }

    public void apply(@NonNull ExtendedSprite sprite) {
        apply(sprite, null);
    }

    public void apply(@NonNull ExtendedSprite sprite, ExtendedSprite previousSprite) {
        if (scale != -1) {
            sprite.setScale(scale);
        }
        if (width != -1) {
            sprite.setWidth(width);
        }
        if (height != -1) {
            sprite.setHeight(height);
        }

        // The origin of the sprite that is using this class is in bottom left, but without its
        // origin being set explicitly in ExtendedSprite to bottom left as touch area does not account for
        // different origins yet. To work around this, we need to flip the position in the Y axis.
        // TODO: When ExtendedSprite supports touch area properly, this should be changed to use bottom-left origin
        float x = xPosition;
        float y = Config.getRES_HEIGHT() - sprite.getHeightScaled() - yPosition;

        if (Float.isNaN(x)) {
            x = previousSprite != null ? previousSprite.getX() + previousSprite.getWidthScaled() : 0;
        }

        if (Float.isNaN(y)) {
            y = Config.getRES_HEIGHT() - sprite.getHeightScaled();
        }

        sprite.setPosition(x, y);
    }
}
