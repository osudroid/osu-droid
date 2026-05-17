package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.osudroid.mods.ModFlashlight;

import org.andengine.entity.Entity;
import org.andengine.entity.modifier.IEntityModifier;
import com.reco1l.andengine.component.UIComponent;
import com.reco1l.framework.Interpolation;

import ru.nsu.ccfit.zuev.osu.Config;


public class FlashLightEntity extends UIComponent {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isTrackingSliders = false;

    private final float followDelay;
    private float nextPX;
    private float nextPY;

    public FlashLightEntity(final ModFlashlight flashlight) {
        super();

        setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        followDelay = flashlight.getFollowDelay();
        mainSprite = new MainFlashLightSprite(flashlight.getSizeMultiplier(), flashlight.isComboBasedSize());
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        attachChild(dimLayer);
    }

    public void onBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void onMouseMove(float pX, float pY) {
        nextPX = FMath.clamp(pX, 0, Config.getRES_WIDTH());
        nextPY = FMath.clamp(pY, 0, Config.getRES_HEIGHT());
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        this.isTrackingSliders = isTrackingSliders;
    }

    public void onUpdate(int combo) {
        dimLayer.onTrackingSliders(isTrackingSliders);
        mainSprite.onUpdate(combo);
    }

    @Override
    protected void onManagedUpdate(float deltaTimeSec) {
        super.onManagedUpdate(deltaTimeSec);

        if (followDelay == 0) {
            setPosition(nextPX, nextPY);
            return;
        }

        float x = getInterpolatedPosition(deltaTimeSec, getX(), nextPX);
        float y = getInterpolatedPosition(deltaTimeSec, getY(), nextPY);

        setPosition(x, y);
    }

    private float getInterpolatedPosition(float deltaTime, float current, float next) {
        return Interpolation.floatAt(Math.min(deltaTime, followDelay), current, next, 0, followDelay, Easing.Out);
    }
}

