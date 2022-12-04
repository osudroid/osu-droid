package com.reco1l.andengine.scenes;
// Created by Reco1l on 26/11/2022, 04:58

import android.view.View;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.Game;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ScheduledTask;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

public class LoaderScene extends BaseScene {

    private Layout layout;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.Loader;
    }

    //--------------------------------------------------------------------------------------------//

    public void complete(Runnable task) {
        layout.onComplete(task);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        layout = new Layout();
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {
    }

    //--------------------------------------------------------------------------------------------//

    public static class Layout extends UIFragment {

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

        public Layout() {
            log = ToastLogger.getLog();
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            setDismissMode(false, false);

            indicator = find("progress");
            text = find("text");

            if (log != null)
                log.clear();

            ToastLogger.setPercentage(-1);
            percentage = -1;

            Animation.of(indicator)
                    .fromAlpha(0)
                    .toAlpha(1)
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

        public void onComplete(Runnable onEnd) {
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
                            .runOnEnd(super::close)
                            .play(200);
                });
            }

            ScheduledTask.run(onEnd, 200);
        }
    }
}
