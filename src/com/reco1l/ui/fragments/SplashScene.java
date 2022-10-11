package com.reco1l.ui.fragments;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edlplan.ui.TriangleEffectView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.UI;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 24/6/22 02:49

public class SplashScene extends UIFragment implements IUpdateHandler {

    private static final String START_SOUND = "welcome_piano";
    private static final String HIT_SOUND = "menuhit";

    public final Scene scene;

    private float logoSize;
    private int lastProgress = 0;

    private final type build;
    private View background, logo, effect;
    private TextView percentText, loadingInfo, buildText;

    private LinearLayout loadingLayout;
    private TriangleEffectView trianglesBottom, trianglesTop;
    private CircularProgressIndicator progressBar;

    private final Runnable hideBuildText =
            () -> new Animation(buildText).moveY(0, -20).fade(1, 0).play(300);

    //--------------------------------------------------------------------------------------------//

    private enum type {
        RELEASE(0),
        DEBUG(R.string.splash_screen_debug_build),
        PRE_RELEASE(R.string.splash_screen_pre_release_build);

        int textId;

        type(int textId) {
            this.textId = textId;
        }
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressWarnings("ConstantConditions")
    public SplashScene() {
        scene = new Scene();

        if ("pre_release".equals(BuildConfig.BUILD_TYPE)) {
            build = type.PRE_RELEASE;
        } else if (BuildConfig.DEBUG) {
            build = type.DEBUG;
        } else {
            build = type.RELEASE;
        }

        super.show();
        scene.registerUpdateHandler(this);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.splash_screen;
    }

    @Override
    protected String getPrefix() {
        return "ss";
    }

    // In this case this doesn't have parent scene since it only appear when the game starts

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        logoSize = Resources.dimen(R.dimen.mainMenuLogoSize);
        resources.loadSound(START_SOUND, "sfx/" + START_SOUND + ".ogg", false);
        resources.loadSound(HIT_SOUND, "sfx/" + HIT_SOUND + ".ogg", false);

        background = find("background");
        loadingLayout = find("bottom");
        progressBar = find("progress");
        buildText = find("buildText");
        percentText = find("percent");
        loadingInfo = find("info");
        effect = find("effect");
        logo = find("logo");

        trianglesBottom = find("triangles1");
        trianglesTop = find("triangles2");

        buildText.setAlpha(0);
        loadingLayout.setAlpha(0);
        trianglesTop.setTriangleColor(Color.WHITE);
        trianglesBottom.setTriangleColor(Color.WHITE);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius((logoSize / 2) * 1.5f);
        drawable.setColor(Color.WHITE);
        effect.setBackground(drawable);

        resources.getSound(START_SOUND).play();

        new Animation(rootView).fade(0, 1f)
                .play(1000);

        new Animation().ofFloat(8f, 1f)
                .runOnUpdate(val -> {
                    trianglesBottom.setTriangleSpeed(val);
                    trianglesTop.setTriangleSpeed(val);
                })
                .play(1000);

        new Animation(trianglesTop).scale(1.2f, 1).rotation(180, 180)
                .play(1000);
        new Animation(trianglesBottom).scale(1.2f, 1)
                .play(1000);

        rootView.postDelayed(() -> {
            new Animation(loadingLayout).fade(0, 1)
                    .play(300);

            if (build.textId != 0) {
                buildText.setText(build.textId);

                new Animation(buildText).moveY(20, 0).fade(0, 1)
                        .runOnEnd(() -> buildText.postDelayed(hideBuildText, 4000))
                        .play(500);
            } else {
                buildText.setVisibility(View.GONE);
            }
        }, 1000);
    }

    //--------------------------------------------------------------------------------------------//

    private final Runnable onComplete = () -> {
        new Animation(loadingLayout).fade(1, 0)
                .play(300);

        buildText.removeCallbacks(hideBuildText);
        hideBuildText.run();

        rootView.postDelayed(() -> {

            new Animation(trianglesBottom).scale(1, 1.2f).fade(0.05f, 0)
                    .play(400);
            new Animation(trianglesTop).scale(1, 1.2f).fade(0.05f, 0).rotation(180, 180)
                    .play(400);

            new Animation(logo).size(Resources.dimen(R.dimen.splashScreenLogoSize), logoSize)
                    .runOnEnd(() -> new Animation(effect).size(logoSize, logoSize * 1.5f).fade(0.1f, 0).play(300))
                    .play(400);

            new Animation(background).fade(1, 0)
                    .runOnEnd(() -> {
                        super.close();
                        new Animation(UI.topBar.body).moveY(-UI.topBar.barHeight, 0).play(300);
                        new Animation(UI.topBar.author).fade(0, 1).moveY(50, 0).play(200);
                    })
                    .play(700);
        }, 300);
    };

    @Override
    public void close() {
        if (!isShowing)
            return;

        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            @Override
            public void run() {
                global.getMainScene().loadMusic();
            }

            @Override
            public void onComplete() {
                mActivity.runOnUiThread(onComplete);
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onUpdate(float pSecondsElapsed) {
        if (!isShowing)
            return;
        int progress = global.getLoadingProgress();

        if (loadingInfo != null) {
            mActivity.runOnUiThread(() -> {
                if (global.getInfo() == null) {
                    loadingInfo.setText("Loading...");
                } else if (global.getInfo() != loadingInfo.getText()) {
                    loadingInfo.setText(global.getInfo());
                }
            });
        }

        if (buildText != null) {
            if (progress != lastProgress) {
                new Animation().ofInt(lastProgress, progress)
                        .runOnUpdate(val -> {
                            progressBar.setProgress(val);
                            percentText.setText(val + "%");
                        })
                        .play(200);
            }
        }
        lastProgress = progress;
    }

    @Override
    public void reset() {
        // Do nothing
    }
}
