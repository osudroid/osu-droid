package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationByModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.annotation.Nullable;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorSprite extends Sprite implements ISliderListener {
    public final float baseSize = Config.getCursorSize() * 2;

    private final ScaleModifier clickInModifier;
    private final ScaleModifier clickOutModifier;
    private final SequenceEntityModifier clickModifier;
    @Nullable private final RotationByModifier rotationModifier;


    public CursorSprite(float pX, float pY, TextureRegion pTextureRegion) {
        super(pX, pY, pTextureRegion);
        setScale(baseSize);

        float clickAnimationTime = 0.5f / 2f;


        clickInModifier = new ScaleModifier(clickAnimationTime, baseSize, baseSize * 1.25f);
        clickOutModifier = new ScaleModifier(clickAnimationTime, baseSize * 1.25f, baseSize);
        clickModifier = new SequenceEntityModifier(clickInModifier.deepCopy(), clickOutModifier.deepCopy());

        if (OsuSkin.get().isRotateCursor()) {
            rotationModifier = new RotationByModifier(14, 360);
            registerEntityModifier(rotationModifier);
        } else {
            rotationModifier = null;
        }
    }


    public void handleClick() {
        unregisterEntityModifiers(m -> m instanceof ScaleModifier);
        clickModifier.reset();
        registerEntityModifier(clickModifier);
    }

    public void update(float pSecondsElapsed) {
        if (getScaleX() > 2f) {
            setScale(Math.max(baseSize, this.getScaleX() - (baseSize * 0.75f) * pSecondsElapsed));
        }

        if (rotationModifier != null && rotationModifier.isFinished()) {
            rotationModifier.reset();
        }
    }

    @Override
    public void onSliderStart() {

    }

    @Override
    public void onSliderTracking() {
        unregisterEntityModifiers(m -> m instanceof ScaleModifier);
        clickInModifier.reset();
        registerEntityModifier(clickInModifier);
    }

    @Override
    public void onSliderEnd() {
        unregisterEntityModifiers(m -> m instanceof ScaleModifier);
        clickOutModifier.reset();
        registerEntityModifier(clickOutModifier);
    }
}
