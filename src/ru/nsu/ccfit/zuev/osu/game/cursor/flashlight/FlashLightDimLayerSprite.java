package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Color;
import com.edlplan.andengine.TextureHelper;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;

public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public final float SLIDER_DIM_ALPHA = 0.75f;

    public FlashLightDimLayerSprite() {
        super(TextureHelper.create1xRegion(Color.BLACK));
        this.setScale(MainFlashLightSprite.TEXTURE_WIDTH, MainFlashLightSprite.TEXTURE_HEIGHT);
        this.setAlpha(SLIDER_DIM_ALPHA);
    }

    public void update(boolean isSliderDimActive) {
        if (isSliderDimActive) {
            this.setAlpha(SLIDER_DIM_ALPHA);
        } else if (this.getAlpha() == SLIDER_DIM_ALPHA) {
            this.registerEntityModifier(new FadeOutModifier(0.05f));
        }
    }
}
