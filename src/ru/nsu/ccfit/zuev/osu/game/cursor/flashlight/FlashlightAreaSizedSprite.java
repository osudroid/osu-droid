package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class FlashlightAreaSizedSprite extends Sprite {
    public static final float BASE_SIZE = 6f;

    public FlashlightAreaSizedSprite(TextureRegion pTextureRegion) {
        super(-MainFlashLightSprite.TEXTURE_WIDTH / 2f, -MainFlashLightSprite.TEXTURE_HEIGHT / 2f, pTextureRegion);
        this.setScale(BASE_SIZE);
    }
}
