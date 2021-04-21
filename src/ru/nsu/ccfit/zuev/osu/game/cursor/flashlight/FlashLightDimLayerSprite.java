package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Bitmap;

import com.edlplan.andengine.TextureHelper;

public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public final float SLIDER_DIM_ALPHA = 0.75f;

    public FlashLightDimLayerSprite() {
        super(TextureHelper.createRegion(Bitmap.createBitmap(MainFlashLightSprite.TEXTURE_WIDTH, MainFlashLightSprite.TEXTURE_HEIGHT, Bitmap.Config.RGB_565)));
        this.setAlpha(SLIDER_DIM_ALPHA);
    }

    public void update(boolean isSliderDimActive) {
        this.setVisible(isSliderDimActive);
    }
}
