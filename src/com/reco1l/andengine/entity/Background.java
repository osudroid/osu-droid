package com.reco1l.andengine.entity;

// Created by Reco1l on 26/6/22 18:22

import com.reco1l.Game;
import com.reco1l.andengine.IAttachableEntity;
import com.reco1l.UI;
import com.reco1l.utils.Animation;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.ease.EaseQuadInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

public class Background implements IAttachableEntity {

    public Rectangle layer;
    public Sprite sprite;

    private Scene parent;
    private TextureRegion defaultTexture;

    private final IEaseFunction interpolator;

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

    public void change(TextureRegion texture) {
        Game.mActivity.runOnUpdateThread(() -> parent.detachChild(sprite));

        if (texture == null) {
            sprite = getScaledSprite(defaultTexture);
        } else {
            sprite = getScaledSprite(texture);
        }

        new Animation().ofFloat(0, 1f)
                .runOnUpdate(val -> sprite.setColor(val, val, val))
                .runOnStart(() -> Game.mActivity.runOnUpdateThread(() -> parent.attachChild(sprite, 0)))
                .play(500);
    }

    //--------------------------------------------------------------------------------------------//

    public void dimIn() {
        if (layer == null)
            return;

        layer.clearEntityModifiers();
        layer.registerEntityModifier(new AlphaModifier(0.4f, 0, 0.3f, interpolator));
    }

    public void dimOut() {
        if (layer == null)
            return;

        layer.clearEntityModifiers();
        layer.registerEntityModifier(new AlphaModifier(0.4f, 0.3f, 0, interpolator));
    }

    public void zoomIn() {
        if (sprite == null)
            return;

        sprite.clearEntityModifiers();
        sprite.registerEntityModifier(new ScaleModifier(0.4f, 1, 1.2f, interpolator));
    }

    public void zoomOut() {
        if (sprite == null || !UI.mainMenu.isMenuShowing)
            return;

        sprite.clearEntityModifiers();
        sprite.registerEntityModifier(new ScaleModifier(0.4f, 1.2f, 1, interpolator));
    }

}
