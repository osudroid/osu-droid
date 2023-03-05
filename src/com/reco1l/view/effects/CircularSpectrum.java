package com.reco1l.view.effects;

// Created by Reco1l on 13/11/2022, 20:54

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.view.RoundLayout;

public class CircularSpectrum extends RoundLayout {

    private Paint mPaint;
    private View mAttachedView;

    private final int mLines = 120;

    private int
            mLineWidth = 8,
            mPeakRate = 600,
            mPeakDownRate = 30,
            mOffsetRadius = 300;

    private int mRotationIndex;

    private float[]
            mPeakLevel,
            mPeakDownLevel;

    private RectF[] mRect;

    //--------------------------------------------------------------------------------------------//

    public CircularSpectrum(@NonNull Context context) {
        super(context);
    }

    public CircularSpectrum(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularSpectrum(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        setConstantInvalidation(true);

        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setAlpha(80);

        mLineWidth = sdp(6);

        mPeakLevel = new float[mLines];
        mPeakDownLevel = new float[mLines];
        mRect = new RectF[mLines];
    }

    //--------------------------------------------------------------------------------------------//

    public void attachTo(View logo) {
        this.mAttachedView = logo;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (mAttachedView != null) {
            setScaleX(mAttachedView.getScaleX());
            setScaleY(mAttachedView.getScaleY());

            mOffsetRadius = (int) (mAttachedView.getWidth() / 2f);

            setX(mAttachedView.getX() - canvas.getWidth() / 2f + mAttachedView.getWidth() / 2f);
            setY(mAttachedView.getY() - canvas.getHeight() / 2f + mAttachedView.getHeight() / 2f);
        }

        if (Game.songService == null) {
            return;
        }
        float[] fft = getFft();

        if (fft != null) {
            update(canvas, fft);
        } else {
            update(canvas, new float[mLines]);
        }
    }

    private float[] getFft() {
        assert Game.songService != null;

        float[] raw = Game.songService.getSpectrum();
        if (raw == null) {
            return null;
        }

        float[] fft = new float[mLines];
        int j = 0;

        for (int i = 1; i < mLines; ++i) {
            j = (j + i) % 40;
            fft[i] = raw[j];
        }
        return fft;
    }

    private void update(Canvas canvas, float[] fft) {
        int i = 0;
        while (i < mLines) {
            float peak = fft[i] * mPeakRate;

            if (peak > mPeakLevel[i]) {
                mPeakLevel[i] = peak;
                mPeakDownLevel[i] = mPeakLevel[i] / mPeakDownRate;
            } else {
                mPeakLevel[i] = Math.max(mPeakLevel[i] - mPeakDownLevel[i], 0);
            }

            drawLine(canvas, i);
            i++;
        }
    }
    
    private void drawLine(Canvas c, int i) {
        float centerX = c.getWidth() / 2f;
        float centerY = c.getHeight() / 2f;

        float half = mLineWidth / 2f;

        int top = (int) (centerY - mOffsetRadius);
        int bottom = (int) (centerY - mOffsetRadius - mPeakLevel[i]);

        int left = (int) (centerX - half);
        int right = (int) (centerX + half);

        if (mRect[i] == null) {
            mRect[i] = new RectF(left, bottom, right, top);
        }
        mRect[i].top = bottom;
        mRect[i].bottom = top;

        c.save();
        c.rotate(i * 3f, centerX, centerY);
        c.drawRect(mRect[i], mPaint);

        mRect[i].top = centerY + mOffsetRadius;
        mRect[i].bottom = centerY + mOffsetRadius + mPeakLevel[i];
        c.drawRect(mRect[i], mPaint);

        c.restore();
    }
}
