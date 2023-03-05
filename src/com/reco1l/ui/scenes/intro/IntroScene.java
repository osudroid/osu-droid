package com.reco1l.ui.scenes.intro;
// Created by Reco1l on 26/11/2022, 06:20

import android.view.View;

import com.reco1l.Game;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.utils.Animation;
import com.reco1l.tables.Res;
import com.reco1l.view.BadgeTextView;
import com.reco1l.view.effects.StripsEffect;

import main.audio.BassSoundProvider;
import com.rimu.R;

public class IntroScene extends BaseScene {

    //----------------------------------------------------------------------------------------//

    public IntroScene() {
        super();
        new IntroFragment().show();
    }

    @Override
    public boolean onBackPress() {
        return true;
    }

    //----------------------------------------------------------------------------------------//

    public static class IntroFragment extends BaseFragment {

        private View
                mLogoBrand,
                mLogoLines;

        private BadgeTextView mText;
        private StripsEffect mEffect;

        private BassSoundProvider mWelcomeSound;

        private boolean mIsTextShowing = false;

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getLayout() {
            return R.layout.layout_intro;
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
            mWelcomeSound = Game.resourcesManager.loadSound("welcome", "sfx/welcome.ogg", false);

            mLogoBrand = find("logoBrand");
            mLogoLines = find("logoLines");
            mEffect = find("effect");
            mText = find("loading");

            mText.setAlpha(0);

            Animation.of(rootView)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .play(1000);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        public void close() {
            if (!isAdded()) {
                return;
            }
            Game.libraryManager.shuffleLibrary();

            if (mIsTextShowing) {
                Animation.of(mText)
                        .toY(20)
                        .toAlpha(0)
                        .play(300);
            }

            Animation.of(mLogoLines)
                    .fromScale(2)
                    .toScale(2.15f)
                    .fromRotation(-50)
                    .toRotation(-70)
                    .toAlpha(0.5f)
                    .runOnStart(mWelcomeSound::play)
                    .runOnEnd(() -> {

                        int size = Res.sdp(250);

                        Animation.of(mLogoLines)
                                .toScale(1)
                                .toSize(size)
                                .toRotation(0)
                                .toAlpha(1)
                                .play(300);

                        Animation.of(mEffect)
                                .toAlpha(0)
                                .play(300);

                        Animation.of(mLogoBrand)
                                .toSize(size)
                                .runOnEnd(() -> {
                                    super.close();

                                    DialogBuilder b = new DialogBuilder("Warning");

                                    b.setMessage("This is an unstable development build it can be full of bugs or errors!\nKeep in mind " +
                                            "rimu! is not osu!droid so please don't report issues at official osu!droid links.\n\n" +
                                            "As alternative you can report a bug or leave your feedback at: \n\n" +
                                            "rimu-beta Discord forum:\n" +
                                            "https://discord.gg/rnbHbsbeFr\n\n" +
                                            "rimu! repository:\n" +
                                            "https://github.com/reco1I/rimu/issues"
                                    );

                                    b.addButton("I understand!", Dialog::close);
                                    new Dialog(b).show();
                                })
                                .play(300);

                        Game.musicManager.play();
                    })
                    .delay(mIsTextShowing ? 300 : 0)
                    .play(2000);
        }

        //--------------------------------------------------------------------------------------------//

        @Override
        public void onEngineUpdate(float elapsed) {
            String text = Game.globalManager.getInfo();
            mText.setText(text);

            if (mIsTextShowing) {
                return;
            }
            mIsTextShowing = true;

            Animation.of(mText)
                    .fromY(20)
                    .toY(0)
                    .toAlpha(1)
                    .play(300);
        }
    }
}
