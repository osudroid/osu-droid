package ru.nsu.ccfit.zuev.osu.game.cursor.flashlight;

import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.ResourceManager;


public class MainFlashLightSprite extends FlashlightAreaSizedSprite {
    private static final TextureRegion DEFAULT_TEXTURE = ResourceManager.getInstance().getTexture("flashlight_cursor");
    public static final int TEXTURE_WIDTH = DEFAULT_TEXTURE.getWidth();
    public static final int TEXTURE_HEIGHT = DEFAULT_TEXTURE.getHeight();
    public final float AREA_CHANGE_FADE_DURATION = 0.8f;
    public float currentSize = BASE_SIZE;


    public MainFlashLightSprite() {
        super(DEFAULT_TEXTURE);
    }

    private void changeArea(float fromScale, float toScale) {
        this.registerEntityModifier(new ScaleModifier(AREA_CHANGE_FADE_DURATION, fromScale, toScale));
    }

    public void onUpdate(int combo) {
        this.handleAreaShrinking(combo);
    }

    public void handleAreaShrinking(int combo) {
        // Area stops shrinking at 200 combo
        if (combo <= 200 && combo % 100 == 0) {
            // For every 100 combo, the size is decreased by 10%
            final float newSize = (1 - 0.1f * combo / 100f) * BASE_SIZE;
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
