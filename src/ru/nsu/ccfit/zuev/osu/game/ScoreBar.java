package ru.nsu.ccfit.zuev.osu.game;

import android.util.Log;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;

import java.util.ArrayList;
import java.util.List;

import org.anddev.andengine.opengl.texture.region.TextureRegion;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class ScoreBar extends GameObject {
    private static float speed = 0.75f;
    private final StatisticV2 stat;
    private final Sprite bg;
    private final Sprite colour;
    private final AnimSprite ki;
    private final float width;
    private float lasthp = 0;

    private float mColourX;

    public ScoreBar(final GameObjectListener listener, final Scene scene,
                    final StatisticV2 stat) {
        this.stat = stat;
        bg = new Sprite(0, 0, ResourceManager.getInstance().getTexture(
                "scorebar-bg"));
        bg.setScaleCenter(0, 0);

        mColourX = Utils.toRes(5);

        if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-0")) {
            List<String> loadedScoreBarTextures = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-" + i))
                    loadedScoreBarTextures.add("scorebar-colour-" + i);
            }

            colour = new AnimSprite(mColourX, Utils.toRes(16), loadedScoreBarTextures.size(),
                    loadedScoreBarTextures.toArray(new String[loadedScoreBarTextures.size()]));
        } else {
            colour = new Sprite(mColourX, Utils.toRes(16),
                    ResourceManager.getInstance().getTexture("scorebar-colour"));
        }
        width = colour.getWidth();
        ki = ResourceManager.getInstance().isTextureLoaded("scorebar-kidanger")
                ? new AnimSprite(0, 0, 0, "scorebar-ki", "scorebar-kidanger", "scorebar-kidanger2")
                : new AnimSprite(0, 0, 0, "scorebar-ki");
        ki.setPosition(Utils.toRes(5) + colour.getWidth() - ki.getWidth() / 2,
                Utils.toRes(16) + colour.getHeight() / 2 - Utils.toRes(58));

        scene.attachChild(ki, 0);
        scene.attachChild(colour, 0);
        scene.attachChild(bg, 0);
    }

    public void setVisible(final boolean visible) {
        bg.setVisible(visible);
        colour.setVisible(visible);
        ki.setVisible(visible);
    }


    @Override
    public void update(final float dt) {
        float hp = stat.getHp();
        if (Math.abs(hp - lasthp) > speed * dt) {
            hp = speed * dt * Math.signum(hp - lasthp) + lasthp;
        }

        TextureRegion texture = colour.getTextureRegion();
        float translationX = width - (width * Math.abs(hp));

        colour.setPosition(mColourX - translationX, colour.getY());
        texture.setTexturePosition((int) -translationX, 0);

        ki.setPosition(Utils.toRes(5) + colour.getWidth() - ki.getWidth() / 2,
                Utils.toRes(16) + colour.getHeight() / 2 - ki.getHeight() / 2);
        ki.setFrame(hp > 0.49 ? 0
                : hp > 0.24 ? 1
                : 2);
        lasthp = hp;
    }

    public void flush() {
        lasthp = stat.getHp();
        update(0);
    }
    
}
