package ru.nsu.ccfit.zuev.osu.game;

import com.reco1l.osu.graphics.ExtendedEntity;
import com.reco1l.osu.graphics.ExtendedSprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class CircleNumber extends ExtendedEntity {


    private int number = 0;

    private boolean isInvalid = false;


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
            var width = 0f;
            var height = 0f;

            var prefix = OsuSkin.get().getHitCirclePrefix();
            var overlap = OsuSkin.get().getHitCircleOverlap();

            for (int i = 0; i < numberStr.length(); i++) {

                var sprite = (ExtendedSprite) getChild(i);
                var textureRegion = ResourceManager.getInstance().getTextureWithPrefix(prefix, String.valueOf(numberStr.charAt(i)));

                if (i > 0) {
                    sprite.setPosition(width - overlap, 0f);
                }
                sprite.setTextureRegion(textureRegion);

                width = sprite.getX() + sprite.getWidth();
                height = Math.max(height, sprite.getHeight());
            }

            setSize(width, height);
        }

        super.onManagedUpdate(pSecondsElapsed);
    }


    public void setNumber(int number) {
        if (this.number == number) {
            return;
        }
        this.number = number;
        isInvalid = true;
    }

}
