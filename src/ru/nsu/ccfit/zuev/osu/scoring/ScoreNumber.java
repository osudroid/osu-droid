package ru.nsu.ccfit.zuev.osu.scoring;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class ScoreNumber extends Entity {
    private float height;

    public ScoreNumber(final float x, final float y, final String text, final float scale, final boolean center) {
        super(x, y);
        float totalWidth = 0;
        for (int i = 0; i < text.length(); i++) {

            var ch = text.charAt(i);
            String textureName;
            Sprite letter;

            if (ch <= '9' && ch >= '0') {
                textureName = String.valueOf(ch);
            } else if (ch == '.' || ch == ',') {
                textureName = "comma";
            } else if (ch == '%') {
                textureName = "percent";
            } else {
                textureName = "x";
            }

            letter = new Sprite(totalWidth * scale, 0, ResourceManager.getInstance().getTextureWithPrefix(OsuSkin.get().getScorePrefix(), textureName));
            letter.setSize(letter.getWidth() * scale, letter.getHeight() * scale);

            totalWidth += letter.getWidth() * scale;
            height = letter.getHeight() * scale;

            attachChild(letter);
        }

        if (center) {
            totalWidth /= 2 * scale;
            for (int i = 0, count = getChildCount(); i < count; i++) {
                var sp = getChild(i);
                sp.setPosition(sp.getX() - totalWidth, sp.getY());
                sp.registerEntityModifier(new SequenceEntityModifier(
                        new ScaleModifier(0.2f, scale, scale * 1.5f),
                        new ScaleModifier(0.4f, scale * 1.5f, scale)));
            }
        }// if center
    }

    // Leaving this to support old usages.
    public void attachToScene(final Scene scene) {
        scene.attachChild(this);
    }

    // Leaving this to support old usages.
    public void detachFromScene(final Scene scene) {
        scene.detachChild(this);
    }

    public float getHeight() {
        return height;
    }
}
