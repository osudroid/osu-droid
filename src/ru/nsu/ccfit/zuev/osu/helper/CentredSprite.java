package ru.nsu.ccfit.zuev.osu.helper;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.TextureRegion;
import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class CentredSprite extends Sprite {

    public CentredSprite(final float pX, final float pY,
                         final TextureRegion pTextureRegion) {
        super(pX - (float) pTextureRegion.getWidth() / 2, pY
                - (float) pTextureRegion.getHeight() / 2, pTextureRegion, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
    }

}
