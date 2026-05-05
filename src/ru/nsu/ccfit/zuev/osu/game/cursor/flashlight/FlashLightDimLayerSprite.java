package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Color;
import com.edlplan.andengine.TextureHelper;
import com.rian.andengine.modifier.UniversalModifier;

public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public final float BASE_SLIDER_DIM_ALPHA = 0.8f;
    private boolean isTrackingSliders;
    private UniversalModifier modifier;

    public FlashLightDimLayerSprite() {
        super(TextureHelper.create1xRegion(Color.BLACK));
        setScale(MainFlashLightSprite.TEXTURE_WIDTH, MainFlashLightSprite.TEXTURE_HEIGHT);
        setAlpha(0);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        if (this.isTrackingSliders == isTrackingSliders) {
            return;
        }

        this.isTrackingSliders = isTrackingSliders;

        if (modifier != null) {
            unregisterEntityModifier(modifier);
        }

        clearEntityModifiers();
        modifier = fadeTo(isTrackingSliders ? BASE_SLIDER_DIM_ALPHA : 0, 0.05f);
    }
}
