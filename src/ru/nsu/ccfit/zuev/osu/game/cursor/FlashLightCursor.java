package ru.nsu.ccfit.zuev.osu.game.cursor;

import android.graphics.Bitmap;

import com.edlplan.andengine.TextureHelper;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;


enum FLConst {
    SLIDER_DIM_ALPHA(0.75f),
    AREA_SHRINK_FADE_DURATION(0.8f),
    TEXTURE_WIDTH(1024),
    TEXTURE_HEIGHT(512),
    BASE_SCALE_SIZE(8f),
    BASE_PX(-TEXTURE_WIDTH.v / 2f),
    BASE_PY(-TEXTURE_HEIGHT.v / 2f);

    float v;
    FLConst(float v) {
        this.v = v;
    }
}

public class FlashLightCursor extends Entity  {
    private final MainFlashLightSprite mainSprite;
    private final FlashLightDimLayerSprite dimLayer;
    private boolean isShowing = false;
    private boolean isSliderDimActive = false;

    public FlashLightCursor(Scene fgScene) {
        TextureRegion tex = ResourceManager.getInstance().getTexture("flashlight_cursor");

        mainSprite = new MainFlashLightSprite(tex);
        dimLayer = new FlashLightDimLayerSprite();

        attachChild(mainSprite);
        fgScene.attachChild(dimLayer, 0);
    }

    public void setSliderDimActive(boolean isSliderDimActive) {
        this.isSliderDimActive = isSliderDimActive;
    }

    public void setShowing(boolean showing) {
        this.isShowing = showing;
    }

    public void updateBreak(boolean isBreak) {
        mainSprite.updateBreak(isBreak);
    }

    public void update(int combo) {
        dimLayer.update(isSliderDimActive);
        mainSprite.update(combo, isShowing);
    }
}

class FlashlightAreaSizedSprite extends Sprite {
    public FlashlightAreaSizedSprite(TextureRegion pTextureRegion) {
        super(FLConst.BASE_PX.v, FLConst.BASE_PY.v, pTextureRegion);
        this.setScale(FLConst.BASE_SCALE_SIZE.v);
    }
}

class MainFlashLightSprite extends FlashlightAreaSizedSprite {
    private float currentSize = FLConst.BASE_SCALE_SIZE.v;

    public MainFlashLightSprite(TextureRegion pTextureRegion) {
        super(pTextureRegion);
    }

    public void update(int combo, boolean isShowing) {
        setVisible(isShowing);
        handleAreaShrinking(combo);
    }

    public void handleAreaShrinking(int combo) {
        // Area stops shrinking at 200 combo
        if (combo <= 200 && combo % 100 == 0) {
            // For every 100 combo, the size is decreased by 20%
            final float newSize = (1 - 0.2f * combo / 100f) * FLConst.BASE_SCALE_SIZE.v;
            this.registerEntityModifier(new ScaleModifier(FLConst.AREA_SHRINK_FADE_DURATION.v, currentSize, newSize));
            currentSize = newSize;
        }
    }

    public void updateBreak(boolean isBreak) {
        float fromScale = isBreak? currentSize : 1.5f * FLConst.BASE_SCALE_SIZE.v;
        float toScale = isBreak? 1.5f * FLConst.BASE_SCALE_SIZE.v : currentSize;

        this.registerEntityModifier(new ScaleModifier(FLConst.AREA_SHRINK_FADE_DURATION.v, fromScale, toScale));
    }
}

class FlashLightDimLayerSprite extends FlashlightAreaSizedSprite {
    public FlashLightDimLayerSprite() {
        super(TextureHelper.createRegion(Bitmap.createBitmap((int) FLConst.TEXTURE_WIDTH.v, (int) FLConst.TEXTURE_HEIGHT.v, Bitmap.Config.RGB_565)));
        this.setAlpha(FLConst.SLIDER_DIM_ALPHA.v);
    }

    public void update(boolean isSliderDimActive) {
        this.setVisible(isSliderDimActive);
    }
}
