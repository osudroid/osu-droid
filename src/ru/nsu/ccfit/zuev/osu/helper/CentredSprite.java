package ru.nsu.ccfit.zuev.osu.helper;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class CentredSprite extends Sprite {

    public CentredSprite(
        final float pX, final float pY, final TextureRegion pTextureRegion) {
        super(pX - pTextureRegion.getWidth() / 2, pY - pTextureRegion.getHeight() / 2, pTextureRegion);
    }

}
