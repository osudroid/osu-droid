package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.reco1l.andengine.sprite.ExtendedSprite;
import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.Config;

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

        float xPosition = previousSprite != null ? previousSprite.getX() + previousSprite.getWidthScaled() : 0;
        float yPosition = Config.getRES_HEIGHT() - sprite.getHeightScaled();

        // The origin of the sprite that is using this class is in bottom left, but without its
        // origin being set explicitly in ExtendedSprite to bottom left as touch area does not account for
        // different origins yet. Similarly, touch area does not account for translations yet.
        // To work around this, we need to flip the offset in the Y axis.
        // TODO: When ExtendedSprite supports touch area properly, this should be changed to use origins and translations
        sprite.setPosition(xPosition + xOffset, yPosition - yOffset);
    }
}
