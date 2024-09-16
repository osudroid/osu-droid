package ru.nsu.ccfit.zuev.osu.helper;

import com.reco1l.osu.graphics.ExtendedSprite;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class CentredSprite extends ExtendedSprite {

    public CentredSprite(final float pX, final float pY, final TextureRegion pTextureRegion) {
        super(pX, pY);
        setTextureRegion(pTextureRegion);
        setOrigin(0.5f, 0.5f);
    }

}
