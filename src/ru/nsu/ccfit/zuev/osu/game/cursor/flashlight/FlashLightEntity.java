package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import com.edlplan.framework.math.FMath;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import ru.nsu.ccfit.zuev.osu.Config;


public class FlashLightEntity extends Entity  {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isTrackingSliders = false;

    private final float areaFollowDelay;
    private IEntityModifier currentModifier = null;
    private float nextPX;
    private float nextPY;

    public FlashLightEntity(final float areaFollowDelay) {
        super(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);

        this.areaFollowDelay = areaFollowDelay;
        mainSprite = new MainFlashLightSprite();
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        attachChild(dimLayer);
    }

    public void onBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void onMouseMove(float pX, float pY) {
        if (nextPX != 0 && nextPY != 0 && currentModifier != null && this.getX() != nextPX && this.getY() != nextPY) {
            unregisterEntityModifier(currentModifier);
        }

        nextPX = FMath.clamp(pX, 0, Config.getRES_WIDTH());
        nextPY = FMath.clamp(pY, 0, Config.getRES_HEIGHT());

        if (areaFollowDelay == 0) {
            setPosition(nextPX, nextPY);
            return;
        }

        currentModifier = new MoveModifier(areaFollowDelay, this.getX(), nextPX, this.getY(), nextPY, EaseExponentialOut.getInstance());

        registerEntityModifier(currentModifier);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        this.isTrackingSliders = isTrackingSliders;
    }

    public void onUpdate(int combo) {
        dimLayer.onTrackingSliders(isTrackingSliders);
        mainSprite.onUpdate(combo);
    }
}

