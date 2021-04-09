package ru.nsu.ccfit.zuev.osu.game.cursor;


import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;

public class FlashLightDimLayer extends Entity {
    private final Sprite sprite;

    public FlashLightDimLayer() {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_dim_layer");
        sprite = new Sprite(-tex.getWidth() / 2f, -tex.getHeight() / 2f, tex);
        float size = 8f;
        sprite.setScale(size);
        sprite.setVisible(false);
        attachChild(sprite);
    }

    public void update(boolean isSliderHold) {
        boolean isSpriteVisible = sprite.isVisible();
        if (isSliderHold && !isSpriteVisible) {
            sprite.setVisible(true);
        } else if (!isSliderHold && isSpriteVisible) {
            sprite.setVisible(false);
        }
    }
}
