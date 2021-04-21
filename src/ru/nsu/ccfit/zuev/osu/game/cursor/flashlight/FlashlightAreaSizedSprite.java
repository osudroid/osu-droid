package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class FlashlightAreaSizedSprite extends Sprite {
    public FlashlightAreaSizedSprite(TextureRegion pTextureRegion) {
        super(FLConst.BASE_PX.v, FLConst.BASE_PY.v, pTextureRegion);
        this.setScale(FLConst.BASE_SCALE_SIZE.v);
    }
}
