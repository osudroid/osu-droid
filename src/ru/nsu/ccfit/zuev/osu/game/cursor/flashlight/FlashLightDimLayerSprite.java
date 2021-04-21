package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Bitmap;

import com.edlplan.andengine.TextureHelper;

public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public FlashLightDimLayerSprite() {
        super(TextureHelper.createRegion(Bitmap.createBitmap((int) FLConst.TEXTURE_WIDTH.v, (int) FLConst.TEXTURE_HEIGHT.v, Bitmap.Config.RGB_565)));
        this.setAlpha(FLConst.SLIDER_DIM_ALPHA.v);
    }

    public void update(boolean isSliderDimActive) {
        this.setVisible(isSliderDimActive);
    }
}
