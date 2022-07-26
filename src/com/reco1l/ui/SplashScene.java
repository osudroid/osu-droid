package com.reco1l.ui;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 24/6/22 02:49

public class SplashScene extends UIFragment implements IUpdateHandler {

    private final GlobalManager global = GlobalManager.getInstance();
    public static SplashScene instance;
    public final Scene scene;

    private View bottomLayout, background, buildInfo, icon;
    private TextView percentText, loadingInfo;
    private ProgressBar progressBar;
    private boolean showBuildText = false;

    private final type build;
    private enum type {
        RELEASE(0),
        DEBUG(R.string.splash_screen_debug_build),
        PRE_RELEASE(R.string.splash_screen_pre_release_build);

        int textId;
        type(int textId) { this.textId = textId; }
    }

    //____________________________________________________________________________________________//

    @Override protected int getLayout() {
        return R.layout.splash_screen;
    }

    @Override protected String getPrefix() {
        return "ss";
    }

    public SplashScene() {
        scene = new Scene();
        instance = this;

        //noinspection ConstantConditions
        build = BuildConfig.BUILD_TYPE.equals("pre_release") ?
                type.PRE_RELEASE : BuildConfig.DEBUG ? type.DEBUG : type.RELEASE;

        show();
        scene.registerUpdateHandler(this);
    }

    //____________________________________________________________________________________________//

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        TextView buildInfoText = find("buildInfoText");
        background = find("background");
        progressBar = find("progress");
        bottomLayout = find("bottom");
        buildInfo = find("buildInfo");
        percentText = find("percent");
        loadingInfo = find("info");
        icon = find("icon");

        showBuildText = build == type.PRE_RELEASE || build == type.DEBUG;

        if (isNull(icon, buildInfo, bottomLayout, progressBar, buildInfoText))
            return;

        if (showBuildText && build.textId != 0)
            buildInfoText.setText(build.textId);

        buildInfo.setAlpha(0f);
        bottomLayout.setAlpha(0f);
        setVisible(showBuildText, buildInfo);

        new Animation(icon).moveY(-50, 0).fade(0, 1).play(300);

        if (showBuildText)
            new Animation(buildInfo).moveY(20, 0).fade(0, 1).delay(1400).play(300);

        new Animation(bottomLayout).moveX(-50, 0).fade(0, 1).delay(1000)
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

        mActivity.runOnUiThread(() -> {
            new Animation(bottomLayout).moveX(0, -50).fade(1, 0)
                    .play(300);

            new Animation(icon).fade(1, 0).delay(300)
                    .play(400);
            new Animation(background).fade(1, 0).delay(300).runOnEnd(super::close)
                    .play(400);
        });
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (!isShowing)
            return;

        if (isNull(percentText, progressBar, loadingInfo))
            return;

        mActivity.runOnUiThread(() -> {

            if (global.getInfo() != null && loadingInfo != null)
                loadingInfo.setText(global.getInfo());

            percentText.setText(global.getLoadingProgress() + "%");
            progressBar.setProgress(global.getLoadingProgress());

            if (buildInfo != null && showBuildText)
                if (global.getLoadingProgress() > 80) {
                    new Animation(buildInfo).moveY(0, -20).fade(1, 0).play(300);
                }

            if (global.getLoadingProgress() == 100) {
                scene.unregisterUpdateHandler(this);
            }
        });
    }


    @Override public void reset() { }
}
