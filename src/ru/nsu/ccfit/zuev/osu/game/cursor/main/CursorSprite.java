package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;

public class CursorSprite extends Sprite {
    public static final float BASE_SIZE = Config.getCursorSize() * 2;

    public CursorSprite(float pX, float pY, TextureRegion pTextureRegion) {
        super(pX, pY, pTextureRegion);
    }

    public void handleClick() {
        this.setScale(BASE_SIZE * 1.25f);
    }

    public void update(float pSecondsElapsed, boolean isShowing) {
        this.setVisible(isShowing);
        this.setScale(BASE_SIZE);

        if (this.getScaleX() > 2f) {
            this.setScale(Math.max(BASE_SIZE, this.getScaleX() - (BASE_SIZE * 0.75f) * pSecondsElapsed));
        }
    }

}
