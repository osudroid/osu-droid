package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;


public class FlashLightEntity extends Entity  {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isTrackingSliders = false;

    private IEntityModifier currentModifier = null;
    private float nextPX;
    private float nextPY;

    public FlashLightEntity() {
        super(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);

        mainSprite = new MainFlashLightSprite();
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        attachChild(dimLayer);
    }

    public void onBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void onMouseMove(float pX, float pY) {
        float flFollowDelay = ModMenu.getInstance().getFLfollowDelay();

        if (flFollowDelay <= 0) {
            this.setPosition(pX, pY);
            return;
        }

        if (nextPX != 0 && nextPY != 0 && currentModifier != null && this.getX() != nextPX && this.getY() != nextPY) {
            this.unregisterEntityModifier(currentModifier);
        }

        currentModifier = new MoveModifier(flFollowDelay, this.getX(), pX, this.getY(), pY, EaseExponentialOut.getInstance());
        nextPX = pX;
        nextPY = pY;

        this.registerEntityModifier(currentModifier);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        this.isTrackingSliders = isTrackingSliders;
    }

    public void onUpdate(int combo) {
        dimLayer.onTrackingSliders(isTrackingSliders);
        mainSprite.onUpdate(combo); }
}

