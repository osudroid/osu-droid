package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameScoreTextShadow extends GameObject {

    private final GameScoreText comboText;

    private final AnimSprite[] letters;

    private final ArrayList<AnimSprite> digits = new ArrayList<>();

    private boolean hasX = false;

    private String text;

    public GameScoreTextShadow(
        float x, float y, final String mask, final float scale, GameScoreText comboText) {
        this.comboText = comboText;
        letters = new AnimSprite[mask.length()];
        float width = 0;

        // Since GameScoreTextShadow is only used for the combo thing im inferring the prefix to the combo one.
        var prefix = OsuSkin.get().getComboPrefix();

        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == '0') {
                letters[i] = new AnimSprite(x + width, y, prefix, null, 10, 0);
                digits.add(letters[i]);
            } else if (mask.charAt(i) == '.') {
                letters[i] = new AnimSprite(x + width, y, prefix, "comma", 1, 0);
            } else if (mask.charAt(i) == '%') {
                letters[i] = new AnimSprite(x + width, y, prefix, "percent", 1, 0);
            } else {
                letters[i] = new AnimSprite(x + width, y, prefix, "x", 1, 0);
                hasX = true;
            }
            letters[i].setSize(letters[i].getWidth() * scale, letters[i].getHeight() * scale);
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

    public void changeText(String text) {
        if (text.equals(this.text)) {
            return;
        }
        int j = 0;
        float digitsWidth = 0;

        var textLength = text.length();
        var digitsSize = digits.size();

        for (int i = 0; i < textLength; i++) {
            if (j >= digitsSize) {
                break;
            }
            var digit = digits.get(j);
            var ch = text.charAt(i);

            if (ch >= '0' && ch <= '9') {
                digit.setVisible(true);
                digit.setFrame(ch - '0');
                digitsWidth += digit.getWidth();
                j++;
            } else if (ch == '*') {
                digit.setVisible(false);
                j++;
            }
        }
        if (hasX) {
            letters[letters.length - 1].setPosition(digits.get(0).getX() + digitsWidth, letters[letters.length - 1].getY());
        }
        // Set previous text if wasn't set yet.
        comboText.changeText(this.text);
        this.text = text;

        letters[0].setAlpha(0.6f);
    }

    public void attachToScene(final Scene scene) {
        scene.attachChild(letters[0], 0);
    }


    @Override
    public void update(final float dt) {
        if (letters[0].getAlpha() > 0) {
            float alpha = letters[0].getAlpha() - dt;
            if (alpha < 0) {
                alpha = 0;
            }

            letters[0].setScale(1.5f - Math.abs(0.6f - alpha));
            letters[0].setPosition(20, Config.getRES_HEIGHT() - letters[0].getHeightScaled() - 20);
            for (final AnimSprite sp : letters) {
                sp.setAlpha(alpha);
            }
        } else {
            comboText.changeText(text);
        }
    }

}
