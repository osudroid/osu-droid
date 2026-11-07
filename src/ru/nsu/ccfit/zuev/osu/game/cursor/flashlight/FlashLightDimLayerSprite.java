package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import android.graphics.Color;
import com.rian.andengine.modifier.UniversalModifier;

import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;

import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public final float BASE_SLIDER_DIM_ALPHA = 0.8f;

    // The dim layer covers this many flashlight textures in each direction around the cursor.
    private static final float AREA_SCALE = 8f;
    private boolean isTrackingSliders;
    private UniversalModifier modifier;

    public FlashLightDimLayerSprite() {
        super(BitmapTextureAtlasTextureRegionFactory.createSolidColor(GlobalManager.getInstance().getEngine().getTextureManager(), Color.BLACK));
        setSize(AREA_SCALE, AREA_SCALE);
        setScale(MainFlashLightSprite.TEXTURE_WIDTH, MainFlashLightSprite.TEXTURE_HEIGHT);
        setAlpha(0);
    }

    public void onTrackingSliders(boolean isTrackingSliders) {
        if (this.isTrackingSliders == isTrackingSliders) {
            return;
        }

        this.isTrackingSliders = isTrackingSliders;

        if (modifier != null) {
            removeModifier(modifier);
            modifier = null;
        }

        clearEntityModifiers();
        modifier = fadeTo(isTrackingSliders ? BASE_SLIDER_DIM_ALPHA : 0, 0.05f).after(e -> modifier = null);
    }
}
