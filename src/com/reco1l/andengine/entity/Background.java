package com.reco1l.andengine.entity;

import com.reco1l.Game;
import com.reco1l.andengine.IAttachableEntity;
import com.reco1l.utils.AsyncExec;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.ease.EaseQuadInOut;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;

public class Background implements IAttachableEntity {

    public Sprite sprite;

    private Scene scene;
    private TextureRegion texture;
    private ScaleModifier scaleModifier;

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(Scene scene, int index) {
        this.scene = scene;

        scene.setBackground(new ColorBackground(0, 0, 0));
        sprite = getScaledSprite();
        scene.attachChild(sprite, index);
    }

    @Override
    public void update() {
        if (sprite != null && sprite.getAlpha() == 0) {
            sprite.registerEntityModifier(new AlphaModifier(0.5f, 0f, 1f));
        }
    }

    //--------------------------------------------------------------------------------------------//

    private Sprite getScaledSprite() {
        if (texture == null) {
            texture = Game.resources.getTexture("menu-background");
        }

        float h = texture.getHeight() * (screenWidth / (float) texture.getWidth());

        return new Sprite(0, (screenHeight - h) / 2f, screenWidth, h, texture);
    }

    //--------------------------------------------------------------------------------------------//

    public void setTexture(String path, boolean animate) {
        texture = null;

        new AsyncExec() {
            public void run() {
                if (!Config.isSafeBeatmapBg()) {
                    texture = Game.resources.loadBackground(path);
                }
                reloadTexture(animate);
            }
        }.execute();
    }

    public void setTexture(TextureRegion texture, boolean animate) {
        this.texture = texture;

        new AsyncExec() {
            public void run() {
                reloadTexture(animate);
            }
        }.execute();
    }

    //--------------------------------------------------------------------------------------------//

    private void reloadTexture(boolean animate) {
        if (sprite != null) {
            Game.runOnUpdateThread(sprite::detachSelf);
        }

        Sprite newSprite = getScaledSprite();

        SyncTaskManager.getInstance().run(() -> {
            scene.attachChild(newSprite, 0);

            if (animate) {
                newSprite.setAlpha(0);
            }
            sprite = newSprite;
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void zoomIn() {
        if (sprite != null) {
            if (scaleModifier != null) {
                sprite.unregisterEntityModifier(scaleModifier);
            }
            scaleModifier = new ScaleModifier(0.4f, 1, 1.2f, EaseQuadInOut.getInstance());
            sprite.registerEntityModifier(scaleModifier);
        }
    }

    public void zoomOut() {
        if (sprite != null) {
            if (scaleModifier != null) {
                sprite.unregisterEntityModifier(scaleModifier);
            }
            scaleModifier = new ScaleModifier(0.4f, 1.2f, 1, EaseQuadInOut.getInstance());
            sprite.registerEntityModifier(scaleModifier);
        }
    }
}
