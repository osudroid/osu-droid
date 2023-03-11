package main.osu.game.cursor.flashlight;

import com.edlplan.framework.math.FMath;
import com.reco1l.management.modding.ModManager;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.util.modifier.ease.EaseExponentialOut;

import main.osu.Config;


public class FlashLightEntity extends Entity  {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isTrackingSliders = false;

    private IEntityModifier currentModifier = null;
    private float nextPX;
    private float nextPY;

    public static final float defaultMoveDelayMS = 120f;
    public static final float defaultMoveDelayS = 0.12f;

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
        float flFollowDelay = ModManager.instance.getCustomFLDelay();

        if (nextPX != 0 && nextPY != 0 && currentModifier != null && this.getX() != nextPX && this.getY() != nextPY) {
            this.unregisterEntityModifier(currentModifier);
        }

        nextPX = FMath.clamp(pX, 0, Config.getRES_WIDTH());
        nextPY = FMath.clamp(pY, 0, Config.getRES_HEIGHT());
        currentModifier = new MoveModifier(flFollowDelay, this.getX(), nextPX, this.getY(), nextPY, EaseExponentialOut.getInstance());

        this.registerEntityModifier(currentModifier);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        this.isTrackingSliders = isTrackingSliders;
    }

    public void onUpdate(int combo) {
        dimLayer.onTrackingSliders(isTrackingSliders);
        mainSprite.onUpdate(combo);
    }
}

