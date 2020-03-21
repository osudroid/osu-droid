package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;

public class GameScoreTextShadow extends GameObject {
    private final AnimSprite[] letters;
    private final ArrayList<AnimSprite> digits = new ArrayList<AnimSprite>();
    private boolean hasX = false;
    private String text = "";

    public GameScoreTextShadow(float x, float y, final String mask,
                               final float scale) {
        letters = new AnimSprite[mask.length()];
        float width = 0;
        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == '0') {
                letters[i] = new AnimSprite(x + width, y, "score-", 10, 0);
                digits.add(letters[i]);
            } else if (mask.charAt(i) == '.') {
                letters[i] = new AnimSprite(x + width, y, 0, "score-comma");
            } else if (mask.charAt(i) == '%') {
                letters[i] = new AnimSprite(x + width, y, 0, "score-percent");
            } else {
                letters[i] = new AnimSprite(x + width, y, 0, "score-x");
                hasX = true;
            }
            letters[i].setSize(letters[i].getWidth() * scale,
                    letters[i].getHeight() * scale);
            width += letters[i].getWidth();
            letters[i].setAlpha(0);
            if (i == 0) {
                x = 0;
                y = 0;
            } else {
                letters[0].attachChild(letters[i]);
            }
        }
        text = "0****";
    }

    public void changeText(final StringBuilder text) {
        if (this.text.equals(text.toString())) {
            return;
        }
        int j = 0;
        float digitsWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            if (j >= digits.size()) {
                break;
            }
            if (text.charAt(i) >= '0' && text.charAt(i) <= '9') {
                digits.get(j).setVisible(true);
                digits.get(j).setFrame(text.charAt(i) - '0');
                digitsWidth += digits.get(j).getWidth();
                j++;
            } else if (text.charAt(i) == '*') {
                digits.get(j).setVisible(false);
                j++;
            }
        }
        if (hasX) {
            letters[letters.length - 1].setPosition(digits.get(0).getX()
                    + digitsWidth, letters[letters.length - 1].getY());
        }
        this.text = text.toString();

        letters[0].setAlpha(0.6f);
    }

    public void attachToScene(final Scene scene) {
        scene.attachChild(letters[0], 0);
    }

    public void detachFromScene() {
        letters[0].detachSelf();
    }


    @Override
    public void update(final float dt) {
        if (letters[0].getAlpha() > 0) {
            float alpha = letters[0].getAlpha() - dt;
            if (alpha < 0) {
                alpha = 0;
            }

            letters[0].setScale(1.5f - Math.abs(0.6f - alpha));
            letters[0].setPosition(Utils.toRes(20), Config.getRES_HEIGHT()
                    - letters[0].getHeightScaled() - Utils.toRes(20));
            for (final AnimSprite sp : letters) {
                sp.setAlpha(alpha);
            }
        }

    }

    public void registerEntityModifier(IEntityModifier modifier) {
        letters[0].registerEntityModifier(modifier);
    }
}
