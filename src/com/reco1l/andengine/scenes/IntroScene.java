package com.reco1l.andengine.scenes;
// Created by Reco1l on 26/11/2022, 06:20

import android.graphics.Color;
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
import com.reco1l.utils.Res;

import ru.nsu.ccfit.zuev.osuplus.R;

public class IntroScene extends BaseScene {


    //----------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.Intro;
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        new IntroFragment().show();
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) { }

    //----------------------------------------------------------------------------------------//

    public static class IntroFragment extends UIFragment {

        private View
                loadingLayout,
                background,
                logo;

        private TextView
                percentText,
                loadingText;

        private TriangleEffectView
                trianglesBottom,
                trianglesTop;

        private CircularProgressIndicator progressIndicator;

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

            Animation.of(trianglesBottom, trianglesTop)
                    .toScale(1.2f)
                    .toAlpha(0)
                    .delay(300)
                    .play(400);

            Animation.of(logo)
                    .toSize(Res.dimen(R.dimen.mainMenuLogoSize))
                    .delay(300)
                    .play(300);

            Animation.of(background)
                    .toAlpha(0)
                    .runOnEnd(super::close)
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
