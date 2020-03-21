package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;

public class GameScoreText {
    private final AnimSprite[] letters;
    private final Map<Character, AnimSprite> characters;
    private final ArrayList<AnimSprite> digits = new ArrayList<AnimSprite>();
    private float scale = 0;
    private boolean hasX = false;

    public GameScoreText(final float x, final float y, final String mask,
                         final float scale) {
        AnimSprite scoreComma = null;
        AnimSprite scorePercent = null;
        AnimSprite scoreX = null;
        letters = new AnimSprite[mask.length()];
        float width = 0;
        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == '0') {
                letters[i] = new AnimSprite(x + width, y, "score-", 10, 0);
                digits.add(letters[i]);
            } else if (mask.charAt(i) == '.') {
                letters[i] = new AnimSprite(x + width, y, 0, "score-comma");
                scoreComma = letters[i];
            } else if (mask.charAt(i) == '%') {
                letters[i] = new AnimSprite(x + width, y, 0, "score-percent");
                scorePercent = letters[i];
            } else {
                letters[i] = new AnimSprite(x + width, y, 0, "score-x");
                scoreX = letters[i];
                hasX = true;
            }
            letters[i].setSize(letters[i].getWidth() * scale,
                    letters[i].getHeight() * scale);
            width += letters[i].getWidth();
        }
        this.scale = scale;
        this.characters = new HashMap<Character, AnimSprite>();
        this.characters.put('.', scoreComma);
        this.characters.put('%', scorePercent);
        this.characters.put('x', scoreX);
    }

    public void changeText(final StringBuilder text) {
        int j = 0;
        float digitsWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            if (j >= digits.size()) {
                break;
            }
            if (text.charAt(i) >= '0' && text.charAt(i) <= '9') {
                int digit = text.charAt(i) - '0';
                digits.get(j).setVisible(true);
                digits.get(j).setFrame(digit);
                digits.get(j).setWidth(digits.get(j).getFrameWidth() * scale);
                digits.get(j).setPosition(digits.get(0).getX() + digitsWidth, digits.get(j).getY());
                digitsWidth += digits.get(j).getWidth();
                j++;
            } else if (text.charAt(i) == '*') {
                digits.get(j).setVisible(false);
                j++;
                // TODO
            } else {
                char character = text.charAt(i);
                if (characters.containsKey(character)) {
                    AnimSprite sprite = characters.get(character);
                    sprite.setPosition(digits.get(0).getX() + digitsWidth, sprite.getY());
                    digitsWidth += sprite.getWidth();
                }
            }
        }
        if (hasX) {
            letters[letters.length - 1].setPosition(digits.get(0).getX()
                    + digitsWidth, letters[letters.length - 1].getY());
        }
    }

    public void attachToScene(final Scene scene) {
        for (final Sprite sp : letters) {
            scene.attachChild(sp, 0);
        }
    }

    public void detachFromScene() {
        for (final Sprite sp : letters) {
            sp.detachSelf();
        }
    }
}
