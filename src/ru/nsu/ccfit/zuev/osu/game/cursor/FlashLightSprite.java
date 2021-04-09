package ru.nsu.ccfit.zuev.osu.game.cursor;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class FlashLightSprite extends Entity{
    private final Sprite sprite;
    private final FlashLightDimLayer dimLayer;
    private boolean showing = false;
    private final float size = 8f;
    private boolean isSliderHold = false;

    public FlashLightSprite(Scene fgScene) {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_cursor");
        sprite = new Sprite(-tex.getWidth() / 2f, -tex.getHeight() / 2f, tex);
        dimLayer = new FlashLightDimLayer();
        sprite.setScale(size);
        attachChild(sprite);
        fgScene.attachChild(dimLayer, 0);
    }

    public void setSliderHold(boolean isSliderHold) {
        this.isSliderHold = isSliderHold;
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public void update(float pSecondsElapsed, int combo) {
        if (showing){
            dimLayer.update(this.isSliderHold);
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
        if (combo > 200) {
            sprite.setScale(0.6f * size);
        } else if (combo > 100){
           sprite.setScale(0.8f * size);
        } else {
            sprite.setScale(1f * size);
        }

    }
}