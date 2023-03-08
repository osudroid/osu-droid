package com.reco1l.ui.scenes.player.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.management.game.GameWrapper;
import com.reco1l.Game;
import com.reco1l.framework.Views;
import com.reco1l.view.RoundLayout;

import main.osu.scoring.StatisticV2;

public class HealthBarView extends RoundLayout implements IPassiveObject {

    private static final float SPEED = 0.75f;

    private ImageView
            mBackground,
            mColour,
            mKi;

    private Rect mColourRect;
    private StatisticV2 mStatistics;

    private final Bitmap[] mKiFrames = new Bitmap[3];

    private float mLastHP = 0;
    private int mMaxBound;

    //--------------------------------------------------------------------------------------------//

    public HealthBarView(@NonNull Context context) {
        super(context);
    }

    public HealthBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HealthBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setRadius(0);

        mBackground = new ImageView(getContext());
        mColour = new ImageView(getContext());
        mKi = new ImageView(getContext());

        addView(mBackground);
        addView(mColour);
        addView(mKi);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode()) {

            Bitmap back = Game.bitmapManager.get("scorebar-bg");
            Bitmap colour = Game.bitmapManager.get("scorebar-colour");

            mColour.setImageBitmap(colour);
            mBackground.setImageBitmap(back);

            mColour.setY(toEngineScale(16));

            Views.size(mBackground,
                    toEngineScale(back.getWidth()),
                    toEngineScale(back.getHeight())
            );

            Views.size(mColour,
                    toEngineScale(colour.getWidth()),
                    toEngineScale(colour.getHeight())
            );

            mKiFrames[0] = Game.bitmapManager.get("scorebar-ki");
            mKiFrames[1] = Game.bitmapManager.get("scorebar-kidanger");
            mKiFrames[2] = Game.bitmapManager.get("scorebar-kidanger2");
        }

        mColour.post(() -> {
            mMaxBound = mColour.getRight();

            mColourRect = new Rect();
            mColourRect.right = mColour.getRight();
            mColourRect.bottom = mColour.getBottom();

            mColour.setClipBounds(mColourRect);

            mKi.setY(mColour.getWidth() / 2f);
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onObjectUpdate(float dt, float sec) {
        if (mStatistics == null || mColourRect == null) {
            return;
        }
        float HP = mStatistics.getHp();

        if (Math.abs(HP - mLastHP) > SPEED * dt) {
            HP = SPEED * dt * Math.signum(HP - mLastHP) + mLastHP;
        }

        int bound = (int) (mMaxBound * HP);

        mColourRect.right = bound;

        float hp = HP;
        Game.activity.runOnUiThread(() -> {
            mColour.setClipBounds(mColourRect);

            mKi.setX(bound - mKi.getWidth() / 2f);
            mKi.setImageBitmap(mKiFrames[hp > 0.49 ? 0 : hp > 0.24 ? 1 : 2]);
        });
        mLastHP = HP;
    }

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        if (wrapper != null) {
            mStatistics = wrapper.statistics;
        }
    }
}
