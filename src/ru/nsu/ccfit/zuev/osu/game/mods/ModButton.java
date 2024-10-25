package ru.nsu.ccfit.zuev.osu.game.mods;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;

public class ModButton extends Sprite {
    private static final float initalScale = 1.4f;
    private static final float selectedScale = 1.8f;
    private static final float initalRotate = 0f;
    private static final float selectedRotate = 5f;

    private GameMod mod;
    private IModSwitcher switcher = null;

    public ModButton(float pX, float pY, GameMod mod) {
        super(Utils.toRes(pX), Utils.toRes(pY), ResourceManager.getInstance().getTexture(GameMod.getTextureName(mod)));
        this.mod = mod;
        setScale(initalScale);
    }

    public void setSwitcher(IModSwitcher switcher) {
        this.switcher = switcher;
    }

    public void setModEnabled(boolean enabled) {
        if (enabled) {
            setScale(selectedScale);
            setRotation(selectedRotate);
            setColor(1, 1, 1);
        } else {
            setScale(initalScale);
            setRotation(initalRotate);
            setColor(0.7f, 0.7f, 0.7f);
        }
    }


    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                 float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown() && switcher != null) {
            setModEnabled(switcher.switchMod(mod));
            return true;
        }

        return false;
    }


}
