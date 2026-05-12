package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.osudroid.mods.ModFlashlight;

import org.andengine.entity.Entity;
import org.andengine.entity.modifier.IEntityModifier;
import com.reco1l.andengine.component.UIComponent;
import com.rian.andengine.modifier.UniversalModifier;

import ru.nsu.ccfit.zuev.osu.Config;


public class FlashLightEntity extends UIComponent {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isTrackingSliders = false;

    private final float areaFollowDelay;
    private UniversalModifier modifier = null;
    private float nextPX;
    private float nextPY;

    public FlashLightEntity(final ModFlashlight flashlight) {
        super();

        setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        areaFollowDelay = flashlight.getFollowDelay();
        mainSprite = new MainFlashLightSprite(flashlight.getSizeMultiplier(), flashlight.isComboBasedSize());
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        attachChild(dimLayer);
    }

    public void onBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void onMouseMove(float pX, float pY) {
        if (nextPX != 0 && nextPY != 0 && modifier != null && this.getX() != nextPX && this.getY() != nextPY) {
            removeModifier(modifier);
            modifier = null;
        }

        nextPX = FMath.clamp(pX, 0, Config.getRES_WIDTH());
        nextPY = FMath.clamp(pY, 0, Config.getRES_HEIGHT());

        if (areaFollowDelay == 0) {
            setPosition(nextPX, nextPY);
            return;
        }

        modifier = moveTo(nextPX, nextPY, areaFollowDelay, Easing.OutExpo).after(e -> modifier = null);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        this.isTrackingSliders = isTrackingSliders;
    }

    public void onUpdate(int combo) {
        dimLayer.onTrackingSliders(isTrackingSliders);
        mainSprite.onUpdate(combo);
    }
}

