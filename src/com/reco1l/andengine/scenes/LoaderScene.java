package com.reco1l.andengine.scenes;

// Created by Reco1l on 26/11/2022, 04:58

import android.view.View;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

public class LoaderScene extends BaseScene {

    private Fragment fragment;
    private Runnable runOnComplete;

    private boolean
            isImmersive = false,
            isTaskCompleted = false,
            isAnimInProgress = false;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.Loader;
    }

    //--------------------------------------------------------------------------------------------//

    public void runOnComplete(Runnable task) {
        runOnComplete = task;
    }

    public void notifyComplete() {
        isTaskCompleted = true;
    }

    public boolean isImmersive() {
        return isImmersive;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        fragment = new Fragment();
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {
        if (!fragment.isShowing()) {
            return;
        }

        if (!isAnimInProgress && isTaskCompleted) {
            isTaskCompleted = false;
            fragment.onFinish(runOnComplete);
            runOnComplete = null;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void show(boolean immersive) {
        isImmersive = immersive;
        if (immersive) {
            UI.topBar.close();
        }
        super.show();
    }

    @Override
    public void show() {
        show(false);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Fragment extends UIFragment {

        private final ArrayList<String> log;

        private CircularProgressIndicator indicator;
        private TextView text;

        private float percentage;

        //----------------------------------------------------------------------------------------//

        @Override
        protected String getPrefix() {
            return "ls";
        }

        @Override
        protected int getLayout() {
            return R.layout.loader_layout;
        }

        @Override
        protected Screens getParent() {
            return Screens.Loader;
        }

        //----------------------------------------------------------------------------------------//

        public Fragment() {
            log = ToastLogger.getLog();
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            indicator = find("progress");
            text = find("text");

            if (log != null) {
                log.clear();
            }

            ToastLogger.setPercentage(-1);
            percentage = -1;

            Game.loaderScene.isAnimInProgress = true;

            Animation.of(indicator)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .runOnEnd(() -> Game.loaderScene.isAnimInProgress = false)
                    .fromScale(0.8f)
                    .toScale(1)
                    .play(200);
        }

        @Override
        public void onUpdate(float elapsed) {
            if (!isShowing || ToastLogger.getPercentage() == percentage) {
                return;
            }
            percentage = ToastLogger.getPercentage();

            if (indicator != null) {
                indicator.setMax(100);
                indicator.setIndeterminate(false);
                indicator.setProgress((int) percentage);
            }

            if (text != null) {
                text.setText((int) percentage + " %");

                if (text.getVisibility() == View.GONE) {
                    text.setVisibility(View.VISIBLE);

                    Animation.of(text)
                            .fromY(50)
                            .toY(0)
                            .fromAlpha(0)
                            .toAlpha(1)
                            .play(180);
                }
            }
        }

        public void onFinish(final Runnable onEnd) {
            if (isShowing) {
                Game.activity.runOnUiThread(() -> {

                    if (text.getVisibility() == View.VISIBLE) {
                        Animation.of(text)
                                .toY(-50)
                                .toAlpha(0)
                                .play(180);
                    }

                    Animation.of(indicator)
                            .toAlpha(0)
                            .toScale(0.8f)
                            .runOnEnd(() -> {
                                super.close();

                                if (onEnd != null) {
                                    onEnd.run();
                                }
                            })
                            .play(200);
                });
            }
        }
    }
}
