package ru.nsu.ccfit.zuev.osu.game;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.skins.StringSkinData;

public class GameScoreText {
    private final AnimSprite[] letters;
    private final Map<Character, AnimSprite> characters;
    private final ArrayList<AnimSprite> digits = new ArrayList<AnimSprite>();
    private float scale = 0;
    private boolean hasX = false;
    private float width;
    private String text;

    public GameScoreText(StringSkinData prefix, final float x, final float y, final String mask,
                         final float scale) {
        AnimSprite scoreComma = null;
        AnimSprite scorePercent = null;
        AnimSprite scoreX = null;
        letters = new AnimSprite[mask.length()];
        float width = 0;
        for (int i = 0; i < mask.length(); i++) {
            if (mask.charAt(i) == '0') {
                letters[i] = new AnimSprite(x + width, y, prefix, null, 10, 0);
                digits.add(letters[i]);
            } else if (mask.charAt(i) == '.') {
                letters[i] = new AnimSprite(x + width, y, prefix, "comma", 1, 0);
                scoreComma = letters[i];
            } else if (mask.charAt(i) == '%') {
                letters[i] = new AnimSprite(x + width, y, prefix, "percent", 1, 0);
                scorePercent = letters[i];
            } else {
                letters[i] = new AnimSprite(x + width, y, prefix, "x", 1, 0);
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

    public void changeText(final String text) {
        if (text.equals(this.text))
            return;
        this.text = text;

        int j = 0;
        width = 0;
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
                digit.setWidth(digit.getFrameWidth() * scale);
                digit.setPosition(digits.get(0).getX() + width, digit.getY());
                width += digit.getWidth();
                j++;
            } else if (ch == '*') {
                digit.setVisible(false);
                j++;
            } else {
                if (characters.containsKey(ch)) {
                    AnimSprite sprite = characters.get(ch);
                    sprite.setPosition(digits.get(0).getX() + width, sprite.getY());
                    width += sprite.getWidth();
                }
            }
        }
        if (hasX) {
            letters[letters.length - 1].setPosition(digits.get(0).getX()
                    + width, letters[letters.length - 1].getY());
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

    public void setPosition(float x, float y){
        float width = 0;
        for (final Sprite sp : letters){
            sp.setPosition(x + width, y);
            width += sp.getWidth();
        }
    }

    public float getWidth() {
        return width;
    }
}
