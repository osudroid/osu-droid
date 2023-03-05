package com.reco1l.ui.scenes.player.fragments;

import androidx.annotation.UiThread;

import com.reco1l.management.game.GameWrapper;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.player.views.IPassiveObject;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Views;
import com.reco1l.view.BadgeTextView;
import com.reco1l.ui.scenes.player.views.ErrorMeterView;
import com.reco1l.ui.scenes.player.views.HealthBarView;
import com.reco1l.ui.scenes.player.views.ScoreNumberView;
import com.reco1l.ui.scenes.player.views.SongProgressView;
import com.reco1l.ui.elements.FPSBadgeView;
import com.reco1l.ui.scenes.player.views.LeaderboardView;

import java.util.Locale;

import main.osu.scoring.StatisticV2;

import com.rimu.R;

public final class GameOverlay extends BaseFragment implements IPassiveObject {

    public static final GameOverlay instance = new GameOverlay();

    private LeaderboardView mLeaderboard;
    private BadgeTextView mInfoText;

    private ScoreNumberView
            mScore,
            mCombo,
            mAccuracy;

    private SongProgressView mProgress;
    private ErrorMeterView mMeter;
    private HealthBarView mHealth;
    private BadgeTextView mURText;

    private GameWrapper mWrapper;

    //--------------------------------------------------------------------------------------------//

    public GameOverlay() {
        super(Scenes.player);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.player_hud_overlay;
    }

    @Override
    protected String getPrefix() {
        return "go";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mLeaderboard = find("leaderboard");
        mInfoText = find("infoText");
        mProgress = find("progress");
        mAccuracy = find("accuracy");
        mHealth = find("health");
        mCombo = find("combo");
        mScore = find("score");
        mMeter = find("meter");
        mURText = find("ur");

        FPSBadgeView counter = find("fps");
        counter.setProvider(Scenes.player);

        // Allowing only to show 5 scores
        Views.height(mLeaderboard, sdp(28) * 5);
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

        mHealth.onObjectUpdate(dt, sec);
        mProgress.onObjectUpdate(dt, sec);
        mLeaderboard.onObjectUpdate(dt, sec);
    }
}
