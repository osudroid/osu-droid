package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationByModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorSprite extends Sprite implements ISliderListener {
    public final float baseSize = Config.getCursorSize() * 2;
    private final float clickAnimationTime = 0.5f / 2f;
    private ParallelEntityModifier previousClickModifier;
    private RotationByModifier currentRotation;
    private final boolean rotate = OsuSkin.get().isRotateCursor();

    public CursorSprite(float pX, float pY, TextureRegion pTextureRegion) {
        super(pX, pY, pTextureRegion);
        setScale(baseSize);
    }

    public ScaleModifier clickInModifier() {
        return new ScaleModifier(clickAnimationTime, getScaleX(), baseSize * 1.25f);
    }

    public ScaleModifier clickOutModifier() {
        return new ScaleModifier(clickAnimationTime, getScaleX(), baseSize);
    }

    public void handleClick() {
        if (previousClickModifier != null) {
            unregisterEntityModifier(previousClickModifier);
            setScale(baseSize);
        }
        registerEntityModifier(
                previousClickModifier = new ParallelEntityModifier(
                        new SequenceEntityModifier(clickInModifier(), clickOutModifier())
                )
        );
    }

    private void rotateCursor() {
        if (currentRotation == null || currentRotation.isFinished()) {
            registerEntityModifier(currentRotation = new RotationByModifier(14, 360));
        }
    }

    public void update(float pSecondsElapsed) {
        if (getScaleX() > 2f) {
            setScale(Math.max(baseSize, this.getScaleX() - (baseSize * 0.75f) * pSecondsElapsed));
        }

        if (rotate) {
            rotateCursor();
        }
    }

    @Override
    public void onSliderStart() {

    }

    @Override
    public void onSliderTracking() {
        registerEntityModifier(clickInModifier());
    }

    @Override
    public void onSliderEnd() {
        registerEntityModifier(clickOutModifier());
    }
}
