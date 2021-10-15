package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Color;
import com.edlplan.andengine.TextureHelper;

import org.anddev.andengine.entity.modifier.AlphaModifier;


public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public final float BASE_SLIDER_DIM_ALPHA = 0.8f;

    public FlashLightDimLayerSprite() {
        super(TextureHelper.create1xRegion(Color.BLACK));
        this.setScale(MainFlashLightSprite.TEXTURE_WIDTH, MainFlashLightSprite.TEXTURE_HEIGHT);
        this.setAlpha(0);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
       float newAlpha = isTrackingSliders? BASE_SLIDER_DIM_ALPHA : 0;
       this.registerEntityModifier(new AlphaModifier(0.05f, this.getAlpha(), newAlpha));
    }
}
