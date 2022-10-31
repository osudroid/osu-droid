package com.reco1l.andengine.entity;

// Created by Reco1l on 26/6/22 18:22

import com.reco1l.Game;
import com.reco1l.andengine.IAttachableEntity;
import com.reco1l.utils.listeners.ModifierListener;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import ru.nsu.ccfit.zuev.osu.Config;

public class Background implements IAttachableEntity {

    public Rectangle layer;
    public Sprite sprite;

    private Scene parent;
    private TextureRegion defaultTexture;

    private final IEaseFunction interpolator;

    private AlphaModifier alphaModifier;
    private ScaleModifier scaleModifier;

    //--------------------------------------------------------------------------------------------//

    public Background() {
        this.interpolator = EaseQuadInOut.getInstance();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(Scene scene, int index) {
        parent = scene;
        defaultTexture = Game.resources.getTexture("menu-background");

        scene.setBackground(new ColorBackground(0, 0, 0));

        sprite = new Sprite(0, 0, defaultTexture);

        layer = new Rectangle(0, 0, screenWidth, screenHeight);
        layer.setColor(0, 0, 0);
        layer.setAlpha(0);

        scene.attachChild(sprite, 0);
        scene.attachChild(layer, index);
    }

    //--------------------------------------------------------------------------------------------//

    private Sprite getScaledSprite(TextureRegion texture) {
        float h = texture.getHeight() * (screenWidth / (float) texture.getWidth());

        return new Sprite(0, (screenHeight - h) / 2f, screenWidth, h, texture);
    }

    //--------------------------------------------------------------------------------------------//

    // TODO Mess with AndEngine
    public void change(String path) {
        TextureRegion texture = defaultTexture;

        if (sprite != null) {
            sprite.setVisible(false);
        }

        if (path != null && !Config.isSafeBeatmapBg()) {
            texture = Game.resources.loadBackground(path).deepCopy();
        }

        Sprite nextSprite = getScaledSprite(texture);
        parent.attachChild(nextSprite, 0);

        nextSprite.registerEntityModifier(new AlphaModifier(0.5f, 0f, 1f, new ModifierListener() {
            public void onModifierStarted(IModifier<IEntity> m, IEntity i) {
                Game.mActivity.runOnUpdateThread(() -> {
                    if (sprite != null) {
                        sprite.detachSelf();
                    }
                });
            }

            public void onModifierFinished(IModifier<IEntity> m, IEntity i) {
                sprite = (Sprite) i;
            }
        }));
    }

    //--------------------------------------------------------------------------------------------//

    public void dimIn() {
        if (layer != null) {
            if (alphaModifier != null) {
                layer.unregisterEntityModifier(alphaModifier);
            }
            alphaModifier = new AlphaModifier(0.4f, 0, 0.3f, interpolator);
            layer.registerEntityModifier(alphaModifier);
        }
    }

    public void dimOut() {
        if (layer != null) {
            if (alphaModifier != null) {
                layer.unregisterEntityModifier(alphaModifier);
            }
            alphaModifier = new AlphaModifier(0.4f, 0.3f, 0, interpolator);
            layer.registerEntityModifier(alphaModifier);
        }
    }

    public void zoomIn() {
        if (sprite != null) {
            if (scaleModifier != null) {
                sprite.unregisterEntityModifier(scaleModifier);
            }
            scaleModifier = new ScaleModifier(0.4f, 1, 1.2f, interpolator);
            sprite.registerEntityModifier(scaleModifier);
        }
    }

    public void zoomOut() {
        if (sprite != null) {
            if (scaleModifier != null) {
                sprite.unregisterEntityModifier(scaleModifier);
            }
            scaleModifier = new ScaleModifier(0.4f, 1.2f, 1, interpolator);
            sprite.registerEntityModifier(scaleModifier);
        }
    }

}
