package ru.nsu.ccfit.zuev.osu.game.cursor;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class FlashLightSprite extends Entity{
    private final Sprite sprite;
    private boolean showing = false;
    private final float size = 8f;
    private float holdX = 0f;
    private float holdY = 0f;

    public FlashLightSprite() {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_cursor");
        sprite = new Sprite(-tex.getWidth() / 2.0f, -tex.getHeight() / 2.0f, tex);
        sprite.setScale(size);
        attachChild(sprite);
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public void setSliderHoldPos(float xPos, float yPos) {
        this.holdX = xPos;
        this.holdY = yPos;
    }

    public void update(float pSecondsElapsed, int combo) {
        if (showing){
            if (!sprite.isVisible())
                sprite.setVisible(true);
            if (sprite.getAlpha() < 1)
                sprite.setAlpha(1);
        } else {
            if (sprite.getAlpha() > 0)
                sprite.setAlpha(Math.max(0, sprite.getAlpha() - 4f * pSecondsElapsed));
            else
                sprite.setVisible(false);
        }
        float leadScale;
        if (combo >= 200) {
            leadScale = 0.5f;
        } else if (combo >= 100) {
            leadScale = 0.7f;
        } else {
            leadScale = 0.9f;
        }
        if (holdX == 0 && holdY == 0) {
            sprite.setScale(leadScale * size);
        } else { sprite.setScale(0.4f * size); }
    }
}