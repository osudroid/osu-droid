package com.reco1l.ui;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.Animator;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 24/6/22 02:49

public class SplashScene extends BaseLayout implements IUpdateHandler {

    private final GlobalManager global = GlobalManager.getInstance();
    public final Scene scene;

    private View bottomLayout, background, buildInfo, icon;
    private TextView percentText, loadingInfo;
    private ProgressBar progressBar;

    //____________________________________________________________________________________________//

    @Override protected int getLayout() {
        return R.layout.splash_screen;
    }

    @Override protected String getPrefix() {
        return "ss";
    }

    public SplashScene() {
        scene = new Scene();
        show();
        scene.registerUpdateHandler(this);
    }

    //____________________________________________________________________________________________//

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        background = find("background");
        progressBar = find("progress");
        bottomLayout = find("bottom");
        buildInfo = find("buildInfo");
        percentText = find("percent");
        loadingInfo = find("info");
        icon = find("icon");

        if (isNull(icon, buildInfo, bottomLayout, progressBar))
            return;

        buildInfo.setAlpha(0f);
        bottomLayout.setAlpha(0f);

        new Animator(icon).moveY(-50, 0).fade(0, 1)
                .play(300);
        new Animator(buildInfo).moveY(20, 0).fade(0, 1).delay(1400)
                .play(300);
        new Animator(bottomLayout).moveX(-50, 0).fade(0, 1).delay(1000)
                .play(300);

        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        if (isNull(buildInfo, bottomLayout, icon, background)) {
            super.close();
            return;
        }

        new Animator(bottomLayout).moveX(0, -50).fade(1, 0)
                .play(300);

        new Animator(icon).fade(1, 0).delay(300)
                .play(400);
        new Animator(background).fade(1, 0).delay(300).runOnEnd(super::close)
                .play(400);
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (!isShowing)
            return;

        mActivity.runOnUiThread(() -> {
            if (global.getInfo() != null && loadingInfo != null)
                loadingInfo.setText(global.getInfo());

            if (percentText != null)
                percentText.setText(global.getLoadingProgress() + "%");

            if (progressBar != null)
                progressBar.setProgress(global.getLoadingProgress());

            if (global.getLoadingProgress() > 80)
                new Animator(buildInfo).moveY(0, -20).fade(1, 0).play(300);

            if (global.getLoadingProgress() == 100) {
                scene.unregisterUpdateHandler(this);
                close();
            }
        });
    }

    @Override public void reset() { }
}
