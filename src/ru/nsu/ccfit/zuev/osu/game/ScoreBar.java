package ru.nsu.ccfit.zuev.osu.game;

import com.reco1l.osu.graphics.AnimatedSprite;
import com.reco1l.osu.graphics.ExtendedSprite;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class ScoreBar extends GameObject {
    private static float speed = 0.75f;
    private final StatisticV2 stat;
    private final Sprite bg;
    private final ExtendedSprite colour;
    private final ExtendedSprite ki;
    private final float width;
    private float lasthp = 0;

    private final TextureRegion[] kiTextures;


    public ScoreBar(final GameObjectListener listener, final Scene scene, final StatisticV2 stat) {
        this.stat = stat;
        bg = new Sprite(0, 0, ResourceManager.getInstance().getTexture(
                "scorebar-bg"));
        bg.setScaleCenter(0, 0);
        if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-0")) {
            List<String> loadedScoreBarTextures = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (ResourceManager.getInstance().isTextureLoaded("scorebar-colour-" + i))
                    loadedScoreBarTextures.add("scorebar-colour-" + i);
            }
            colour = new AnimatedSprite(loadedScoreBarTextures.toArray(new String[0]));
        } else {
            colour = new ExtendedSprite();
            colour.setTextureRegion(ResourceManager.getInstance().getTexture("scorebar-colour"));
        }
        width = colour.getWidth();
        colour.setAdjustSizeWithTexture(false);
        colour.setPosition(5, 5);

        ki = new ExtendedSprite();
        ki.setTextureRegion(ResourceManager.getInstance().getTextureIfLoaded("scorebar-ki"));
        ki.setPosition(5 + colour.getWidth() - ki.getWidth() / 2, 16 + colour.getHeight() / 2 - 58);

        if (ResourceManager.getInstance().isTextureLoaded("scorebar-kidanger")) {
            kiTextures = new TextureRegion[]{
                ResourceManager.getInstance().getTextureIfLoaded("scorebar-ki"),
                ResourceManager.getInstance().getTextureIfLoaded("scorebar-kidanger"),
                ResourceManager.getInstance().getTextureIfLoaded("scorebar-kidanger2")
            };
        } else {
            kiTextures = null;
        }

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

        colour.setWidth(width * hp);

        ki.setPosition(5 + colour.getWidth() - ki.getWidth() / 2, 16 + colour.getHeight() / 2 - ki.getHeight() / 2);

        if (kiTextures != null) {
            ki.setTextureRegion(kiTextures[hp > 0.49 ? 0 : hp > 0.24 ? 1 : 2]);
        }

        lasthp = hp;
    }

    public void flush() {
        lasthp = stat.getHp();
        update(0);
    }
    
}
