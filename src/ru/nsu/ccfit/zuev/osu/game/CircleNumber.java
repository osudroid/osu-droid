package ru.nsu.ccfit.zuev.osu.game;

import com.reco1l.osu.graphics.container.LinearContainer;
import com.reco1l.osu.graphics.ExtendedSprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CircleNumber extends LinearContainer {


    private int number = 0;

    private boolean isInvalid = false;


    public CircleNumber() {
        setSpacing(-OsuSkin.get().getHitCircleOverlap());
    }


    private void allocateSprites(int count) {
        if (count < getChildCount()) {
            for (int i = getChildCount() - 1; i >= count; i--) {
                detachChild(getChild(i));
            }
        } else {
            for (int i = getChildCount(); i < count; i++) {
                attachChild(new ExtendedSprite());
            }
        }
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {

        if (isInvalid) {
            isInvalid = false;

            var numberStr = String.valueOf(Math.abs(number));

            allocateSprites(numberStr.length());

            var prefix = OsuSkin.get().getHitCirclePrefix();

            for (int i = 0; i < numberStr.length(); i++) {

                var sprite = (ExtendedSprite) getChild(i);
                var textureRegion = ResourceManager.getInstance().getTextureWithPrefix(prefix, String.valueOf(numberStr.charAt(i)));

                sprite.setTextureRegion(textureRegion);
            }
        }

        super.onManagedUpdate(pSecondsElapsed);
    }


    public void setNumber(int number) {
        if (this.number == number) {
            return;
        }
        this.number = number + 10;
        isInvalid = true;
    }

}
