package com.reco1l.ui.scenes.player.fragments;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.UiThread;

import com.caverock.androidsvg.SVGImageView;
import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.annotation.Direction;
import com.reco1l.annotation.Legacy;
import com.reco1l.management.game.GameWrapper;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.scenes.player.views.IPassiveObject;
import com.reco1l.ui.scenes.player.views.ScoreNumberView;
import com.reco1l.ui.scenes.summary.views.PlayerArrowView;
import com.reco1l.framework.Animation;
import com.rimu.R;

import main.osu.game.BreakPeriod;
import main.osu.scoring.StatisticV2;

public class BreakOverlay extends BaseFragment implements IPassiveObject {

    public static final BreakOverlay instance = new BreakOverlay();

    private PlayerArrowView[] mArrows;

    private SVGImageView mMarkIcon;
    private TextView mAccuracyText;
    private LinearLayout mInfoLayout;
    private ScoreNumberView mTimeText;

    private GameWrapper mWrapper;
    private BreakPeriod mCurrentPeriod;

    @Legacy
    private boolean
            mIsOver,
            mIsBreak;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.player_break_overlay;
    }

    @Override
    protected String getPrefix() {
        return "bo";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {

        mArrows = new PlayerArrowView[] {
                find("arrow0"),
                find("arrow1"),
                find("arrow2"),
                find("arrow3")
        };

        mMarkIcon = find("mark");
        mInfoLayout = find("info");
        mTimeText = find("leftTime");
        mAccuracyText = find("accuracy");

        mTimeText.setAlpha(0);
        mInfoLayout.setAlpha(0);

        for (PlayerArrowView arrow : mArrows) {
            arrow.setAlpha(0);
        }
    }

    private void loadStatistics() {
        if (mWrapper == null) {
            return;
        }

        StatisticV2 s = mWrapper.statistics;

        if (s != null) {
            mMarkIcon.setImageAsset("svg/ranking-" + s.getMark() + ".svg");
            mAccuracyText.setText(String.format("%.2f%%", s.getAccuracy() * 100f));
        }
    }

    public void display(BreakPeriod period) {
        if (!isLoaded()) {
            return;
        }

        mIsOver = false;
        mIsBreak = true;
        mCurrentPeriod = period;

        Game.activity.runOnUiThread(this::loadStatistics);

        Animation.of(mTimeText)
                 .fromScale(0.5f)
                 .toScale(1)
                 .fromAlpha(0)
                 .toAlpha(1)
                 .interpolate(Easing.OutExpo)
                 .play(400);

        Animation.of(mInfoLayout)
                 .fromY(50)
                 .toY(0)
                 .fromAlpha(0)
                 .toAlpha(1)
                 .play(400);

        for (PlayerArrowView arrow : mArrows) {
            Animation anim = Animation.of(arrow);

            if (arrow.getDirection() == Direction.LEFT_TO_RIGHT) {
                anim.fromX(-arrow.getWidth());
            }
            else {
                anim.fromX(arrow.getWidth());
            }
            anim.interpolate(Easing.OutExpo);
            anim.toX(0);
            anim.toAlpha(1);

            if (arrow.getWidth() > sdp(80)) { // Means larger arrows
                anim.play(350);
            }
            else {
                anim.play(400);
            }
        }
    }

    public void hide() {
        if (!isLoaded()) {
            return;
        }

        Animation.of(mTimeText)
                 .toScale(0.5f)
                 .toAlpha(0)
                 .interpolate(Easing.InExpo)
                 .play(400);

        Animation.of(mInfoLayout)
                 .toY(50)
                 .toAlpha(0)
                 .play(400);

        for (PlayerArrowView arrow : mArrows) {
            Animation anim = Animation.of(arrow);

            if (arrow.getDirection() == Direction.LEFT_TO_RIGHT) {
                anim.toX(-arrow.getWidth());
            }
            else {
                anim.toX(arrow.getWidth());
            }
            anim.interpolate(Easing.InExpo);
            anim.toAlpha(0);

            if (arrow.getWidth() > sdp(80)) {
                anim.play(400);
            }
            else {
                anim.play(350);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        mWrapper = wrapper;
    }

    @Override
    @UiThread
    public void onObjectUpdate(float dt, float sec) {
        if (mCurrentPeriod == null) {
            return;
        }

        int left = (int) (mCurrentPeriod.getEndTime() - sec);

        if (left <= 0) {
            mCurrentPeriod = null;
            mIsBreak = false;
            mIsOver = true;
            hide();
            return;
        }

        if (isLoaded()) {
            mTimeText.setText("" + left);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public boolean isOver() {
        boolean wasOver = mIsOver;
        mIsOver = false;

        return wasOver;
    }

    @Legacy
    public boolean isBreak() {
        return mIsBreak;
    }
}
