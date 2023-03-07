package com.reco1l.ui.scenes.player.fragments;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.UiThread;

import com.caverock.androidsvg.SVGImageView;
import com.edlplan.framework.easing.Easing;
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

            arrow.setAlpha(0);
            arrow.post(() -> {
                Animation anim = Animation.of(arrow);

                if (arrow.getRotationY() == 0) {
                    anim.fromX(- arrow.getWidth());
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
                    anim.delay(50);
                    anim.play(400);
                }
            });
        }
    }

    @Override
    protected void onPost() {
        if (mWrapper != null) {
            setGameWrapper(mWrapper);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        mWrapper = wrapper;

        if (isLoaded()) {
            StatisticV2 s = mWrapper.statistics;

            if (s != null) {
                mMarkIcon.setImageAsset("svg/ranking-" + s.getMark() + ".svg");
                mAccuracyText.setText(String.format("%.2f%%", s.getAccuracy() * 100f));
            }
        }
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
            close();
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

    //--------------------------------------------------------------------------------------------//

    public boolean show(BreakPeriod period) {
        if (isAdded()) {
            return false;
        }
        mIsOver = false;
        mIsBreak = true;
        mCurrentPeriod = period;
        return super.show();
    }

    @Override
    public boolean show() {
        Logging.e(this, "Call show(BreakPeriod) instead!");
        return false;
    }

    @Override
    public void close() {
        if (!isAdded()) {
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

            if (arrow.getRotationY() == 0) {
                anim.toX(- arrow.getWidth());
            }
            else {
                anim.toX(arrow.getWidth());
            }
            anim.interpolate(Easing.InExpo);
            anim.toAlpha(0);

            if (arrow.getWidth() > sdp(80)) {
                anim.play(400);
                anim.delay(50);
            }
            else {
                if (arrow == mArrows[mArrows.length - 1]) {
                    anim.runOnEnd(super::close);
                }
                anim.play(400);
            }
        }
    }
}
