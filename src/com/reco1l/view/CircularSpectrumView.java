package com.reco1l.view;

// Created by Reco1l on 13/11/2022, 20:54

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.utils.Resources;

import ru.nsu.ccfit.zuev.osu.Config;

public class CircularSpectrumView extends View {

    private Paint paint;
    private LogoView logo;

    private final int lines = 120;

    private int
            radius = 300,
            lineWidth = 8,
            peakRate = 600,
            peakDownRate = 30;

    private int rotationIndex;

    private float[]
            peakLevel,
            peakDownLevel;

    private RectF[] rectF;

    //--------------------------------------------------------------------------------------------//

    public CircularSpectrumView(Context context) {
        super(context);
        init();
    }

    public CircularSpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        createPaint(false);

        if (!isInEditMode()) {
            lineWidth = Resources.sdp(5);
        }

        peakLevel = new float[lines];
        peakDownLevel = new float[lines];
        rectF = new RectF[lines];
    }

    private void createPaint(boolean dark) {
        if (paint != null) {
            if (dark && paint.getColor() == Color.WHITE) {
                return;
            }
            if (!dark && paint.getColor() == Color.BLACK) {
                return;
            }
        }
        paint = new Paint();

        paint.setColor(dark ? Color.WHITE : Color.BLACK);
        paint.setDither(Config.isUseDither());
        paint.setAlpha(64);
    }

    //--------------------------------------------------------------------------------------------//

    public void attachToLogo(LogoView logo) {
        this.logo = logo;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setPaintColor(int color) {
        paint.setColor(color);
    }

    public void setPaintAlpha(float alpha) {
        paint.setAlpha((int) (255 * MathUtils.clamp(alpha, 0f, 1f)));
    }

    public void setLinesWidth(float width) {
        paint.setStrokeWidth(width);
    }

    public void setPeakRate(int multiplier) {
        peakRate = multiplier;
        invalidate();
    }

    public void setPeakDownRate(int multiplier) {
        peakDownRate = multiplier;
        invalidate();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (logo != null) {
            setScaleX(logo.getScaleX());
            setScaleY(logo.getScaleY());

            radius = (int) (logo.getWidth() / 2f);
        }

        if (Game.musicManager.isPlaying()) {
            handleRotation();
        }
        createPaint(UI.background.isDark());

        if (Game.songService != null) {
            float[] fft = getFft();

            if (fft != null) {
                update(canvas, fft);
            } else {
                update(canvas, new float[lines]);
            }
        }
        super.onDraw(canvas);
        invalidate();
    }

    private void handleRotation() {
        if (!Game.timingWrapper.isNextBeat()) {
            return;
        }
        rotationIndex += 10;

        if (rotationIndex > lines) {
            rotationIndex -= lines;
        }
    }

    private float[] getFft() {
        float[] raw = Game.songService.getSpectrum();

        if (raw == null) {
            return null;
        }
        float[] rotated = new float[lines];

        for (int i = 0; i <= lines - 1; ++i) {
            rotated[(i + rotationIndex) % lines] = raw[i];
        }
        return rotated;
    }

    private void update(Canvas canvas, float[] fft) {
        int i = 0;
        while (i < lines) {
            float peak = fft[i] * peakRate;

            if (peak > peakLevel[i]) {
                peakLevel[i] = peak;
                peakDownLevel[i] = peakLevel[i] / peakDownRate;
            } else {
                peakLevel[i] = Math.max(peakLevel[i] - peakDownLevel[i], 0);
            }

            drawLine(canvas, i);
            i++;
        }
    }
    
    private void drawLine(Canvas canvas, int i) {
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;

        float half = lineWidth / 2f;
        
        float cy = centerY - radius;
        float dy = centerY - radius - peakLevel[i];

        float cx = centerX - half;
        float dx = centerX + half;

        if (rectF[i] == null) {
            rectF[i] = new RectF((int) cx, (int) dy, (int) dx, (int) cy);
        }
        rectF[i].top = dy;
        rectF[i].bottom = cy;

        canvas.save();
        canvas.rotate(i * 3f, centerX, centerY);
        canvas.drawRoundRect(rectF[i], half, half, paint);

        rectF[i].top = centerY + radius;
        rectF[i].bottom = centerY + radius + peakLevel[i];
        canvas.drawRoundRect(rectF[i], half, half, paint);

        canvas.restore();
    }
}
