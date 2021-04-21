package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;


public class FlashLightEntity extends Entity  {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isShowing = false;
    private boolean isSliderDimActive = false;

    public FlashLightEntity(Scene fgScene) {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_cursor");

        mainSprite = new MainFlashLightSprite(tex);
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        fgScene.attachChild(dimLayer, 0);
    }

    public void setSliderDimActive(boolean isSliderDimActive) {
        this.isSliderDimActive = isSliderDimActive;
    }

    public void setShowing(boolean showing) {
        this.isShowing = showing;
    }

    public void updateBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void update(int combo) {
        dimLayer.update(isSliderDimActive);
        mainSprite.update(combo, isShowing);
    }
}

