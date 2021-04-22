package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;


public class MainFlashLightSprite extends FlashlightAreaSizedSprite {
    public static final float BASE_SIZE = 8f;
    public static final int TEXTURE_WIDTH = 1024;
    public static final int TEXTURE_HEIGHT = TEXTURE_WIDTH / 2;
    public final float AREA_CHANGE_FADE_DURATION = 0.8f;
    public float currentSize = BASE_SIZE;

    public MainFlashLightSprite(TextureRegion pTextureRegion) {
        super(pTextureRegion);
    }

    public void update(int combo, boolean isShowing) {
        this.setVisible(isShowing);
        this.handleAreaShrinking(combo);
    }

    public void changeArea(float fromScale, float toScale) {
        this.registerEntityModifier(new ScaleModifier(AREA_CHANGE_FADE_DURATION, fromScale, toScale));
    }

    public void handleAreaShrinking(int combo) {
        // Area stops shrinking at 200 combo
        if (combo <= 200 && combo % 100 == 0) {
            // For every 100 combo, the size is decreased by 20%
            final float newSize = (1 - 0.2f * combo / 100f) * BASE_SIZE;
            this.changeArea(currentSize, newSize);
            currentSize = newSize;
        }
    }

    public void updateBreak(boolean isBreak) {
        float fromScale = isBreak? currentSize : 1.5f * BASE_SIZE;
        float toScale = isBreak? 1.5f * BASE_SIZE : currentSize;

        this.changeArea(fromScale, toScale);
    }
}
