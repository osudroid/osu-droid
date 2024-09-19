package ru.nsu.ccfit.zuev.osu.helper;

import com.reco1l.osu.graphics.ExtendedSprite;

import org.anddev.andengine.opengl.texture.region.TextureRegion;

// TODO: Keeping this class for compatibility, consider using ExtendedSprite directly.
public class CentredSprite extends ExtendedSprite {

    public CentredSprite(final float pX, final float pY, final TextureRegion pTextureRegion) {
        super();
        setTextureRegion(pTextureRegion);
        setOrigin(0.5f, 0.5f);
        setPosition(pX, pY);
    }

}
