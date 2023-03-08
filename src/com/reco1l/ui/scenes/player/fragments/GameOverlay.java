package com.reco1l.ui.scenes.player.fragments;

import android.widget.RelativeLayout;

import androidx.annotation.UiThread;

import com.reco1l.management.game.GameWrapper;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.player.views.IPassiveObject;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;
import com.reco1l.tools.Views;
import com.reco1l.view.BadgeTextView;
import com.reco1l.ui.scenes.player.views.ErrorMeterView;
import com.reco1l.ui.scenes.player.views.HealthBarView;
import com.reco1l.ui.scenes.player.views.ScoreNumberView;
import com.reco1l.ui.scenes.player.views.SongProgressView;
import com.reco1l.ui.elements.FPSBadgeView;
import com.reco1l.ui.scenes.player.views.LeaderboardView;

import java.util.Locale;

import main.osu.scoring.StatisticV2;

import com.reco1l.view.RoundedImageView;
import com.rimu.R;

public final class GameOverlay extends BaseFragment implements IPassiveObject {

    public static final GameOverlay instance = new GameOverlay();

    private LeaderboardView mLeaderboard;
    private RoundedImageView mSkipButton;
    private BadgeTextView mInfoText;

    private ScoreNumberView
            mScore,
            mCombo,
            mAccuracy;

    private SongProgressView mProgress;
    private ErrorMeterView mMeter;
    private HealthBarView mHealth;
    private BadgeTextView mURText;

    private RelativeLayout mMainBody;

    private GameWrapper mWrapper;

    private boolean
            mSkipActioned,
            mShowingSkipButton;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.player_hud_overlay;
    }

    @Override
    protected String getPrefix() {
        return "hud";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {

        mLeaderboard = find("leaderboard");
        mInfoText = find("infoText");
        mProgress = find("progress");
        mAccuracy = find("accuracy");
        mSkipButton = find("skip");
        mMainBody = find("main");
        mHealth = find("health");
        mCombo = find("combo");
        mScore = find("score");
        mMeter = find("meter");
        mURText = find("ur");

        FPSBadgeView counter = find("fps");
        counter.setProvider(Scenes.player);

        Views.height(mLeaderboard, sdp(28) * 5);

        mShowingSkipButton = false;
        mSkipButton.setAlpha(0);

        mMainBody.setAlpha(0);
        mMainBody.setScaleX(1.2f);
        mMainBody.setScaleY(1.2f);

        bindTouch(mSkipButton, () -> {
            unbindTouch(mSkipButton);
            mSkipActioned = true;

            Animation.of(mSkipButton)
                     .toY(50)
                     .toAlpha(0)
                     .play(200);

            Animation.of(mMainBody)
                    .toScale(1)
                    .toAlpha(1)
                    .play(200);
        });
    }

    @Override
    protected void onPost() {
        if (mWrapper != null) {
            setGameWrapper(mWrapper);

            mInfoText.setAlpha(1);
            if (mWrapper.isReplaying) {
                mInfoText.setText("Watching " + mWrapper.playerName + " replay");
            }
            else if (mWrapper.isUnranked) {
                mInfoText.setText("This play is unranked");
            }
            else {
                mInfoText.setAlpha(0);
            }

            mShowingSkipButton = mWrapper.skipTime > 1;

            if (mShowingSkipButton) {
                Animation.of(mSkipButton)
                        .fromY(50)
                        .toY(0)
                        .toAlpha(1)
                        .play(200);
            } else {
                unbindTouch(mSkipButton);

                Animation.of(mMainBody)
                         .toScale(1)
                         .toAlpha(1)
                         .play(200);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        mWrapper = wrapper;

        if (isLoaded()) {
            mLeaderboard.setGameWrapper(mWrapper);
            mProgress.setGameWrapper(mWrapper);
            mMeter.setGameWrapper(mWrapper);
            mHealth.setGameWrapper(mWrapper);
        }
    }

    @Override
    public void clear() {
        mWrapper = null;

        if (isLoaded()) {
            mLeaderboard.clear();
            mProgress.clear();
            mMeter.clear();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean wasSkipButtonActioned() {
        boolean actioned = mSkipActioned;
        if (actioned) {
            mSkipActioned = false;
        }
        return mShowingSkipButton && actioned;
    }

    public void onAccuracyChange(float accuracy) {
        mMeter.putErrorAt(accuracy);
    }

    //--------------------------------------------------------------------------------------------//

    @Override @UiThread
    public void onObjectUpdate(float dt, float sec) {
        if (!isLoaded() || mWrapper == null) {
            return;
        }

        StatisticV2 s = mWrapper.statistics;

        if (s != null) {
            mCombo.setText(s.getCombo() + "x");
            mScore.setText("" + s.getAutoTotalScore());
            mAccuracy.setText(String.format("%.2f%%", s.getAccuracy() * 100f));
            mURText.setText(String.format(Locale.ENGLISH, "%.2f UR", s.getUnstableRate()));
        }

        if (sec > mWrapper.skipTime && mShowingSkipButton) {
            mShowingSkipButton = false;
            unbindTouch(mSkipButton);

            Animation.of(mSkipButton)
                    .toY(50)
                    .toAlpha(0)
                    .play(200);

            Animation.of(mMainBody)
                     .toScale(1)
                     .toAlpha(1)
                     .play(200);
        }

        mHealth.onObjectUpdate(dt, sec);
        mProgress.onObjectUpdate(dt, sec);
        mLeaderboard.onObjectUpdate(dt, sec);
    }
}
