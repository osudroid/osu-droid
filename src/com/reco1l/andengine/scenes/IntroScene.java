package com.reco1l.andengine.scenes;
// Created by Reco1l on 26/11/2022, 06:20

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.edlplan.ui.TriangleEffectView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.Game;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class IntroScene extends BaseScene {


    //----------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return null;
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        new Fragment().show();
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) { }

    //----------------------------------------------------------------------------------------//

    public static class Fragment extends UIFragment {

        private View
                loadingLayout,
                background,
                logo;

        private TextView
                percentText,
                loadingText,
                buildText;

        private TriangleEffectView
                trianglesBottom,
                trianglesTop;

        private CircularProgressIndicator progressIndicator;

        private Runnable post;

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getLayout() {
            return R.layout.intro_layout;
        }

        @Override
        protected String getPrefix() {
            return "ss";
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            setDismissMode(false, false);

            Game.resources.loadSound("welcome_piano", "sfx/welcome_piano.ogg", false);

            logo = find("logo");
            buildText = find("buildText");
            background = find("background");

            progressIndicator = find("progress");
            loadingLayout = find("bottom");
            percentText = find("percent");
            loadingText = find("info");

            trianglesBottom = find("triangles1");
            trianglesTop = find("triangles2");

            trianglesTop.setTriangleColor(Color.WHITE);
            trianglesBottom.setTriangleColor(Color.WHITE);

            Animation.of(rootView)
                    .runOnStart(() -> Game.resources.getSound("welcome_piano").play())
                    .fromAlpha(0)
                    .toAlpha(1)
                    .play(1000);

            Animation.ofFloat(8f, 1f)
                    .runOnUpdate(val -> {
                        trianglesBottom.setTriangleSpeed((float) val);
                        trianglesTop.setTriangleSpeed((float) val);
                    })
                    .play(1000);

            Animation.of(trianglesTop, trianglesBottom)
                    .fromScale(1.2f)
                    .toScale(1)
                    .play(1000);

            Animation.of(loadingLayout)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .delay(1000)
                    .play(300);

            //noinspection ConstantConditions
            if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals("pre_release")) {

                //noinspection ConstantConditions
                if (BuildConfig.DEBUG) {
                    buildText.setText(R.string.splash_screen_debug_build);
                } else {
                    buildText.setText(R.string.splash_screen_pre_release_build);
                }

                post = () -> Animation.of(buildText)
                        .toY(-20)
                        .toAlpha(0)
                        .play(300);

                Animation.of(buildText)
                        .fromY(20)
                        .toY(0)
                        .fromAlpha(0)
                        .toAlpha(1)
                        .delay(1000)
                        .play(500);

                buildText.postDelayed(post, 5000);
            } else {
                buildText.setVisibility(View.GONE);
            }
        }

        //--------------------------------------------------------------------------------------------//

        @Override
        public void close() {
            if (!isShowing) {
                return;
            }

            new AsyncExec() {
                public void run() {
                    Game.mainScene.loadMusic();
                }
            }.execute();

            Animation.of(loadingLayout)
                    .toAlpha(0)
                    .play(300);

            if (buildText.getAlpha() == 1) {
                buildText.removeCallbacks(post);
                buildText.animate().cancel();

                if (post != null) {
                    post.run();
                }
            }

            Animation.of(trianglesBottom, trianglesTop)
                    .toScale(1.2f)
                    .toAlpha(0)
                    .delay(300)
                    .play(400);

            Animation.of(logo)
                    .toSize(Resources.dimen(R.dimen.mainMenuLogoSize))
                    .delay(300)
                    .play(300);

            Animation.of(background)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        super.close();
                        Log.i("IS", "Tried to close");
                    })
                    .delay(300)
                    .play(500);
        }

        //--------------------------------------------------------------------------------------------//

        @Override
        public void onUpdate(float elapsed) {
            if (!isShowing || isNull(loadingText, progressIndicator, percentText)) {
                return;
            }
            int progress = Game.global.getLoadingProgress();

            loadingText.setText(Game.global.getInfo());
            progressIndicator.setProgress(progress);
            percentText.setText(progress + "%");
        }
    }
}
