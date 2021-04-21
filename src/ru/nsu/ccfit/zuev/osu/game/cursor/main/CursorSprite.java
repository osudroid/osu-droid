package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class CursorSprite extends Sprite {

    public CursorSprite(float pX, float pY, TextureRegion pTextureRegion) {
        super(pX, pY, pTextureRegion);
    }

    public void handleClick() {
        this.setScale(CursorConst.CURSOR_SIZE.v * 1.25f);
    }

    public void update(float pSecondsElapsed, boolean isShowing) {
        this.setVisible(isShowing);
        this.setScale(CursorConst.CURSOR_SIZE.v);

        if (this.getScaleX() > 2f) {
            this.setScale(Math.max(CursorConst.CURSOR_SIZE.v, this.getScaleX() - (CursorConst.CURSOR_SIZE.v * 0.75f) * pSecondsElapsed));
        }
    }

}
