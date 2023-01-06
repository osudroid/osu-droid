package com.reco1l.scenes;
// Created by Reco1l on 26/11/2022, 06:20

import android.view.View;
import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.tables.Res;
import com.reco1l.view.effects.StripsEffect;

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
    protected void onSceneUpdate(float secondsElapsed) {}

    //----------------------------------------------------------------------------------------//

    public static class IntroFragment extends BaseFragment {

        private View
                logoBrand,
                logoLines;

        private TextView loading;
        private StripsEffect effect;

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getLayout() {
            return R.layout.intro_layout;
        }

        @Override
        protected String getPrefix() {
            return "il";
        }

        @Override
        protected boolean isOverlay() {
            return true;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            Game.resourcesManager.loadSound("welcome_piano", "sfx/welcome_piano.ogg", false);

            logoBrand = find("logoBrand");
            logoLines = find("logoLines");
            loading = find("loading");
            effect = find("effect");

            Animation.of(rootView)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .play(1000);
        }

        //--------------------------------------------------------------------------------------------//

        @Override
        public void close() {
            if (!isAdded()) {
                return;
            }
            Game.libraryManager.shuffleLibrary();

            if (loading.getVisibility() == View.VISIBLE) {
                Animation.of(loading)
                        .toY(20)
                        .toAlpha(0)
                        .play(300);
            }

            Animation.of(logoLines)
                    .fromScale(2)
                    .toScale(2.15f)
                    .fromRotation(-50)
                    .toRotation(-70)
                    .toAlpha(0.5f)
                    .runOnStart(() -> Game.resourcesManager.getSound("welcome_piano").play())
                    .runOnEnd(() -> {

                        int size = Res.sdp(250);

                        Animation.of(logoLines)
                                .toScale(1)
                                .toSize(size)
                                .toRotation(0)
                                .toAlpha(1)
                                .play(300);

                        Animation.of(effect)
                                .toAlpha(0)
                                .play(300);

                        Animation.of(logoBrand)
                                .toSize(size)
                                .runOnEnd(super::close)
                                .play(300);

                        Game.musicManager.play();
                    })
                    .delay(loading.getVisibility() == View.VISIBLE ? 300 : 0)
                    .play(2000);
        }

        //--------------------------------------------------------------------------------------------//

        @Override
        public void onUpdate(float sec) {
            if (!isLoaded()) {
                return;
            }
            int progress = Game.globalManager.getLoadingProgress();
            String info = Game.globalManager.getInfo();

            if (info != null && loading.getVisibility() != View.VISIBLE) {
                loading.setVisibility(View.VISIBLE);

                Animation.of(loading)
                        .fromY(20)
                        .toY(0)
                        .fromAlpha(0)
                        .toAlpha(1)
                        .play(300);
            }
            loading.setText(info + "\n(" + progress + "%)");
        }
    }
}
