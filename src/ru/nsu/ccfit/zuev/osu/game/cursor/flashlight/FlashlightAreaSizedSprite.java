package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.Anchor;

import org.andengine.opengl.texture.region.TextureRegion;

public class FlashlightAreaSizedSprite extends UISprite {
    public static final float BASE_SIZE = 6f;

    public FlashlightAreaSizedSprite(TextureRegion pTextureRegion) {
        super();
        setOrigin(Anchor.Center);
        setScale(BASE_SIZE);
        setTextureRegion(pTextureRegion);
    }
}
