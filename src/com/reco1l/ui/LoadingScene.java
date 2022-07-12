package com.reco1l.ui;

import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.Animation;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 7/7/22 01:17

public class LoadingScene extends BaseLayout implements IUpdateHandler {

    public final Scene scene;
    private final ArrayList<String> log;

    private CircularProgressIndicator progress;
    private TextView text;

    private float percentage;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "ls";
    }

    @Override
    protected int getLayout() {
        return R.layout.loading_screen;
    }

    //--------------------------------------------------------------------------------------------//

    public LoadingScene() {
        scene = new Scene();
        log = ToastLogger.getLog();

        Rectangle dim = new Rectangle(0, 0, Config.getRES_WIDTH(), Config.getRES_HEIGHT());
        dim.setColor(0, 0, 0);
        dim.setAlpha(0.3f);
        scene.attachChild(dim);
        scene.registerUpdateHandler(this);
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        progress = find("progress");
        text = find("text");

        if (log != null)
            log.clear();

        ToastLogger.setPercentage(-1);
        percentage = -1;

        new Animation(progress).fade(0, 1)
                .play(200);
        new Animation(text).moveY(-40, 0).fade(0, 1)
                .delay(200)
                .play(150);
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (ToastLogger.getPercentage() == percentage || isNull(progress, text))
            return;

        if (progress.isIndeterminate())
            progress.setIndeterminate(false);

        progress.setMax(100);
        progress.setProgress((int) percentage);

        percentage = ToastLogger.getPercentage();
        text.setText("Importing beatmaps... (" + (int) percentage + "%)"); //TODO add translation here
    }

    @Override
    public void show() {
        if (isShowing)
            return;

        TextureRegion texture;

        if (resources.getTexture("::background") != null) {
            texture = resources.getTexture("::background");
        } else {
            texture = resources.getTexture("menu-background");
        }

        float H = texture.getHeight() * (screenWidth / (float) texture.getWidth());
        Sprite background = new Sprite(0, (screenHeight - H) / 2f, screenWidth, H, texture);

        scene.setBackground(new SpriteBackground(background));

        engine.setScene(scene);
        mActivity.runOnUiThread(super::show);
    }

    @Override
    public void reset() { }
}
