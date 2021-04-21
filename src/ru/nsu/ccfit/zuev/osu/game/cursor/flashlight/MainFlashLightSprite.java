package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FLConst;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashlightAreaSizedSprite;

public class MainFlashLightSprite extends FlashlightAreaSizedSprite {
    private float currentSize = FLConst.BASE_SCALE_SIZE.v;

    public MainFlashLightSprite(TextureRegion pTextureRegion) {
        super(pTextureRegion);
    }

    public void update(int combo, boolean isShowing) {
        this.setVisible(isShowing);
        this.handleAreaShrinking(combo);
    }

    public void shrinkArea(float fromScale, float toScale) {
        this.registerEntityModifier(new ScaleModifier(FLConst.AREA_SHRINK_FADE_DURATION.v, fromScale, toScale));
    }

    public void handleAreaShrinking(int combo) {
        // Area stops shrinking at 200 combo
        if (combo <= 200 && combo % 100 == 0) {
            // For every 100 combo, the size is decreased by 20%
            final float newSize = (1 - 0.2f * combo / 100f) * FLConst.BASE_SCALE_SIZE.v;
            this.shrinkArea(currentSize, newSize);
            currentSize = newSize;
        }
    }

    public void updateBreak(boolean isBreak) {
        float fromScale = isBreak? currentSize : 1.5f * FLConst.BASE_SCALE_SIZE.v;
        float toScale = isBreak? 1.5f * FLConst.BASE_SCALE_SIZE.v : currentSize;

        this.shrinkArea(fromScale, toScale);
    }
}
