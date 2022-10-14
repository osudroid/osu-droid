package com.reco1l.andengine.entity;

// Created by Reco1l on 11/10/22 19:45

import static com.reco1l.utils.Animation.Interpolate.VALUE_ANIMATOR;

import android.animation.TimeInterpolator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.EasingHelper;
import com.reco1l.Game;
import com.reco1l.andengine.IAttachableEntity;
import com.reco1l.utils.Animation;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

public class BeatMarker implements IAttachableEntity {

    private Sprite spriteLeft, spriteRight;

    private Animation fadeUp, fadeDown;

    private boolean isAlternateMode = false;
    private int cursor = 0;

    //--------------------------------------------------------------------------------------------//

    @Override
    public void draw(Scene scene, int index) {
        Scene layer = new Scene();
        layer.setBackgroundEnabled(false);

        TextureRegion texture = Game.resources.getTexture("border-gradient");
        texture.setHeight(screenHeight);

        spriteLeft = new Sprite(0, 0, texture);
        spriteLeft.setAlpha(0);

        spriteRight = new Sprite(screenWidth - texture.getWidth(), 0, texture);
        spriteRight.setAlpha(0);
        spriteRight.setRotation(180);

        layer.attachChild(spriteLeft);
        layer.attachChild(spriteRight);

        scene.attachChild(layer, index);

        Animation.ValueAnimationListener<Float> listener = layer::setAlpha;

        fadeUp = new Animation().ofFloat(0f, 1f).runOnUpdate(listener);
        fadeDown = new Animation().ofFloat(1f, 0f).runOnUpdate(listener);

        fadeUp.interpolator(Easing.InOutQuad).interpolatorMode(VALUE_ANIMATOR);
        fadeDown.interpolator(Easing.InOutQuad).interpolatorMode(VALUE_ANIMATOR);
    }

    public void setAlternateMode(boolean bool) {
        this.isAlternateMode = bool;
    }

    @Override
    public void update() {
        if (Game.songService != null) {
            float level = Game.songService.getLevel() * 2f;

            spriteLeft.setAlpha(level);
            spriteRight.setAlpha(level);
        }
    }

    public void onBpmUpdate(float bpm) {
        long upTime = (long) (bpm * 0.07f);
        long downTime = (long) (bpm * 0.9f);

        if (fadeUp != null && fadeDown != null) {

            if (isAlternateMode) {
                fadeUp.runOnStart(() -> {
                    cursor = cursor == 0 ? 1 : 0;

                    spriteLeft.setVisible(cursor == 0);
                    spriteRight.setVisible(cursor == 1);
                });
            } else {
                fadeUp.runOnStart(() -> {
                   spriteRight.setVisible(true);
                   spriteLeft.setVisible(true);
                });
            }

            fadeUp.play(upTime);
            fadeDown.delay(upTime).play(downTime);
        }

    }
}
