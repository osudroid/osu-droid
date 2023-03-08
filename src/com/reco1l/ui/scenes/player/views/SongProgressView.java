package com.reco1l.ui.scenes.player.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.framework.drawing.Dimension;
import com.reco1l.management.game.GameWrapper;
import com.reco1l.view.RoundLayout;
import com.reco1l.framework.drawing.CompoundRect;

public class SongProgressView extends RoundLayout implements IPassiveObject {

    private CompoundRect
            mProgress,
            mBackground;

    private float mTime;
    private float mStartTime;
    private float mPassedTime;


    //--------------------------------------------------------------------------------------------//

    public SongProgressView(@NonNull Context context) {
        super(context);
    }

    public SongProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SongProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setConstantInvalidation(true);
        setRadius(0);

        mBackground = new CompoundRect();
        mBackground.paint.setColor(Color.WHITE);
        mBackground.paint.setAlpha(25);

        mProgress = new CompoundRect(mBackground);
        mProgress.paint.setAlpha(255);
    }

    @Override
    protected void onDimensionChange(Dimension dimens) {
        super.onDimensionChange(dimens);

        mBackground.rect.right = getWidth();
        mBackground.rect.bottom = getHeight();

        mProgress.rect.bottom = getHeight();
    }

    @Override
    protected void onManagedDraw(Canvas canvas) {
        mBackground.drawTo(canvas);
        mProgress.drawTo(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        mTime = wrapper.time;
        mStartTime = wrapper.startTime;
    }

    @Override
    public void onObjectUpdate(float dt, float sec) {
        if (mPassedTime >= mStartTime) {
            mPassedTime = Math.min(mTime, mPassedTime + dt);

            mProgress.rect.right = (int) (getWidth() * (mPassedTime - mStartTime) / (mTime - mStartTime));
        } else {
            mPassedTime = Math.min(mStartTime, mPassedTime + dt);

            mProgress.rect.right = (int) (getWidth() * mPassedTime / mStartTime);
        }
    }
}
