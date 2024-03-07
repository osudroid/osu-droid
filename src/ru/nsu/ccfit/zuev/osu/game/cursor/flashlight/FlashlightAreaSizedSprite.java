package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class FlashlightAreaSizedSprite extends Sprite {
    public static final float BASE_SIZE = 6f;

    public FlashlightAreaSizedSprite(TextureRegion pTextureRegion) {
        super(-MainFlashLightSprite.TEXTURE_WIDTH / 2f, -MainFlashLightSprite.TEXTURE_HEIGHT / 2f, pTextureRegion, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        this.setScale(BASE_SIZE);
    }
}
