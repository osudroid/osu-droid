package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;



public class FlashLightEntity extends Entity  {
    private static final float DEFAULT_MOVE_DELAY = 0.12f;
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isShowing = false;
    private boolean isSliderDimActive = false;

    public FlashLightEntity() {
        mainSprite = new MainFlashLightSprite();
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        attachChild(dimLayer);
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

    public void updatePositionByValue(float pX, float pY) {
        this.registerEntityModifier(
            new MoveModifier(DEFAULT_MOVE_DELAY, this.getX(), pX, this.getY(), pY, EaseExponentialOut.getInstance())
        );
    }

    public void update(int combo) {
        dimLayer.update(isSliderDimActive);
        mainSprite.update(combo, isShowing);
    }
}

