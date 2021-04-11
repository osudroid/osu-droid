package ru.nsu.ccfit.zuev.osu.game.cursor;


import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
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
        attachChild(sprite);
    }

    public void update(boolean isSliderHoldAndActive) {
        float FL_SLIDER_DIM_FADE_DURATION = 0.05f;
        float currentSpriteAlpha = sprite.getAlpha();

        if (isSliderHoldAndActive && currentSpriteAlpha == 0) {
            sprite.registerEntityModifier(new AlphaModifier(FL_SLIDER_DIM_FADE_DURATION, 0, 1));
        } else if (!isSliderHoldAndActive && currentSpriteAlpha == 1) {
            sprite.registerEntityModifier(new AlphaModifier(FL_SLIDER_DIM_FADE_DURATION, 1, 0));
        }
    }
}
