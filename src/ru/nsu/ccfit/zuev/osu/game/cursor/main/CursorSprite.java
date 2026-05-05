package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.reco1l.andengine.sprite.UISprite;
import com.rian.andengine.modifier.ModifierType;
import com.rian.andengine.modifier.UniversalModifier;

import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import javax.annotation.Nullable;

import kotlin.Unit;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorSprite extends UISprite implements ISliderListener {
    public final float baseSize = Config.getCursorSize() * 2;
    private final float clickAnimationTime = 0.25f;
    @Nullable private UniversalModifier rotationModifier;


    public CursorSprite(float pX, float pY, TextureRegion pTextureRegion) {
        super();

        setPosition(pX, pY);
        setScale(baseSize);
        setTextureRegion(pTextureRegion);

        if (OsuSkin.get().isRotateCursor()) {
            rotationModifier = rotateTo(360, 14);
        } else {
            rotationModifier = null;
        }
    }


    public void handleClick() {
        clearModifiers(ModifierType.ScaleXY);
        setScale(baseSize);

        beginModifierSequence(sequence -> {
            sequence.scaleTo(baseSize * 1.25f, clickAnimationTime)
                    .then()
                    .scaleTo(baseSize, clickAnimationTime);

            return Unit.INSTANCE;
        });
    }

    public void update(float pSecondsElapsed) {
        if (getScaleX() > 2f) {
            setScale(Math.max(baseSize, this.getScaleX() - (baseSize * 0.75f) * pSecondsElapsed));
        }

        if (rotationModifier != null && rotationModifier.isFinished()) {
            rotationModifier = rotateTo(360, 14);
        }
    }

    @Override
    public void onSliderStart() {

    }

    @Override
    public void onSliderTracking() {
        clearModifiers(ModifierType.ScaleXY);
        setScale(baseSize);
        scaleTo(baseSize * 1.25f, clickAnimationTime);
    }

    @Override
    public void onSliderEnd() {
        clearEntityModifiers();
        setScale(baseSize * 1.25f);
        scaleTo(baseSize, clickAnimationTime);
    }
}
