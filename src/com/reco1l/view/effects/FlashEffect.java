package com.reco1l.view.effects;

// Created by Reco1l on 15/11/2022, 19:28

import static android.graphics.Color.*;
import static android.graphics.Shader.TileMode.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.global.Game;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Animation.UpdateListener;
import com.reco1l.view.RoundLayout;

public class FlashEffect extends RoundLayout {

    private Paint
            mLeftPaint,
            mRightPaint;

    private Paint mCursor;

    private float mRectWidth;

    //--------------------------------------------------------------------------------------------//

    public FlashEffect(@NonNull Context context) {
        super(context);
    }

    public FlashEffect(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FlashEffect(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        setConstantInvalidation(true);
        setRadius(0);

        mLeftPaint = new Paint();
        mLeftPaint.setAlpha(0);

        mRightPaint = new Paint();
        mRightPaint.setAlpha(0);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLayoutChange(ViewGroup.LayoutParams params) {
        mRectWidth = getWidth() / 4f;

        LinearGradient left = new LinearGradient(0, 0, mRectWidth, 0, WHITE, TRANSPARENT, CLAMP);
        mLeftPaint.setShader(left);

        LinearGradient right = new LinearGradient(getWidth() - mRectWidth, 0, getWidth(), 0, TRANSPARENT, WHITE, CLAMP);
        mRightPaint.setShader(right);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (Game.timingWrapper.isNextBeat()) {
            onNextBeat(Game.timingWrapper.getBeatLength());
        }

        int h = canvas.getHeight();
        int w = canvas.getWidth();

        canvas.drawRect(0, 0, mRectWidth, h, mLeftPaint);
        canvas.drawRect(w - mRectWidth, 0, w, h, mRightPaint);
    }

    private void onNextBeat(float length) {
        boolean isKiai = Game.timingWrapper.isKiai();

        long in = (long) (length * 0.07);
        long out = (long) (length * 0.9);

        if (!isKiai) {
            short beat = Game.timingWrapper.getBeat();

            if (beat == 3) {
                playAnimation(mLeftPaint, in, out);
                playAnimation(mRightPaint, in, out);
            }
            return;
        }

        mCursor = mCursor == mLeftPaint ? mRightPaint : mLeftPaint;
        playAnimation(mCursor, in, out);
    }

    private void playAnimation(Paint paint, long in, long out) {
        UpdateListener listener = value -> paint.setAlpha((int) value);

        Animation.ofInt(0, 70)
                .runOnUpdate(listener)
                .play(in);

        Animation.ofInt(70, 0)
                .runOnUpdate(listener)
                .delay(in)
                .play(out);
    }
}
