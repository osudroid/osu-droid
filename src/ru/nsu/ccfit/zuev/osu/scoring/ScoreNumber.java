package ru.nsu.ccfit.zuev.osu.scoring;

import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class ScoreNumber {
    private final Sprite[] letters;
    private float x, y, height;

    public ScoreNumber(final float x, final float y, final String text,
                       final float scale, final boolean center) {
        this.x = x;
        this.y = y;
        letters = new Sprite[text.length()];
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) <= '9' && text.charAt(i) >= '0') {
                letters[i] = new Sprite(x + width * scale, y, ResourceManager
                        .getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), String.valueOf(text.charAt(i))));
            } else if (text.charAt(i) == '.' || text.charAt(i) == ',') {
                letters[i] = new Sprite(x + width * scale, y, ResourceManager
                        .getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "comma"));
            } else if (text.charAt(i) == '%') {
                letters[i] = new Sprite(x + width * scale, y, ResourceManager
                        .getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "percent"));
            } else {
                letters[i] = new Sprite(x + width * scale, y, ResourceManager
                        .getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), "x"));
            }
            letters[i].setSize(letters[i].getWidth() * scale,
                    letters[i].getHeight() * scale);
            width += letters[i].getWidth() * scale;
            height = letters[i].getHeight() * scale;
        }

        if (center) {
            width /= 2 * scale;
            for (final Sprite sp : letters) {
                sp.setPosition(sp.getX() - width, sp.getY());
                sp.registerEntityModifier(new SequenceEntityModifier(
                        new ScaleModifier(0.2f, scale, scale * 1.5f),
                        new ScaleModifier(0.4f, scale * 1.5f, scale)));
            }
        }// if center
    }

    public void attachToScene(final Scene scene) {
        for (final Sprite sp : letters) {
            scene.attachChild(sp);
        }
    }

    public void detachFromScene(final Scene scene) {
        for (final Sprite sp : letters) {
            scene.detachChild(sp);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getHeight() {
        return height;
    }
}
