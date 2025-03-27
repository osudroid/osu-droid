package ru.nsu.ccfit.zuev.osu.game.mods;

import com.rian.osu.mods.IModUserSelectable;

import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;

public class ModButton extends Sprite {
    private static final float unselectedScale = 1.4f;
    private static final float selectedScale = 1.8f;
    private static final float unselectedRotation = 0f;
    private static final float selectedRotation = 5f;

    private final IModUserSelectable mod;
    private final IModSwitcher switcher;

    public ModButton(float pX, float pY, IModUserSelectable mod, IModSwitcher switcher) {
        super(Utils.toRes(pX), Utils.toRes(pY), ResourceManager.getInstance().getTexture(mod.getTextureName()));

        this.mod = mod;
        this.switcher = switcher;

        setScale(unselectedScale);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            setScale(selectedScale);
            setRotation(selectedRotation);
            setColor(1, 1, 1);
        } else {
            setScale(unselectedScale);
            setRotation(unselectedRotation);
            setColor(0.7f, 0.7f, 0.7f);
        }
    }


    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
                                 float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown() && switcher != null) {
            setEnabled(switcher.switchMod(mod));
            return true;
        }

        return false;
    }


}
