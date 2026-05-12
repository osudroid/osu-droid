package ru.nsu.ccfit.zuev.osu.game.cursor.main;

import com.reco1l.andengine.Anchor;
import com.reco1l.andengine.sprite.UISprite;
import com.rian.andengine.modifier.ModifierType;
import com.rian.andengine.modifier.OnModifierFinished;
import com.rian.andengine.modifier.UniversalModifierSequence;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.game.ISliderListener;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CursorSprite extends UISprite implements ISliderListener {
    public final float baseSize = Config.getCursorSize() * 2;
    private final float clickAnimationTime = 0.25f;

    private final OnModifierFinished rotationModifierFinished = e -> {
        setRotation(0);
        startRotationModifierLoop();
    };

    private final Function1<UniversalModifierSequence, Unit> clickSequence = sequence -> {
        sequence.scaleTo(baseSize * 1.25f, clickAnimationTime)
                .then()
                .scaleTo(baseSize, clickAnimationTime);

        return Unit.INSTANCE;
    };

    public CursorSprite() {
        super();

        setAnchor(Anchor.Center);
        setOrigin(Anchor.Center);
        setScale(baseSize);
        setTextureRegion(ResourceManager.getInstance().getTexture("cursor"));
    }

    @Override
    protected void onLoadComplete() {
        startRotationModifierLoop();
    }

    public void handleClick() {
        clearModifiers(ModifierType.ScaleXY);
        setScale(baseSize);
        beginModifierSequence(clickSequence);
    }

    public void update(float pSecondsElapsed) {
        if (getScaleX() > 2f) {
            setScale(Math.max(baseSize, this.getScaleX() - (baseSize * 0.75f) * pSecondsElapsed));
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
        clearModifiers(ModifierType.ScaleXY);
        setScale(baseSize * 1.25f);
        scaleTo(baseSize, clickAnimationTime);
    }

    private void startRotationModifierLoop() {
        if (OsuSkin.get().isRotateCursor()) {
            clearModifiers(ModifierType.Rotation);
            rotateTo(360, 14).after(rotationModifierFinished);
        }
    }
}
