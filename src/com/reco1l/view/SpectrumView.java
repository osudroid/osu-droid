package com.reco1l.view;

// Created by Reco1l on 13/11/2022, 20:54

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import ru.nsu.ccfit.zuev.osu.Config;

public class SpectrumView extends View {

    private Paint paint;

    private int
            radius,
            linesRadius,
            lines = -1,
            peakRate = 900,
            peakDownRate = 20;

    private float
            lineWidth = 12.0f,
            lineDistance = 1f;

    private float[]
            fft,
            peakLevel,
            peakDownLevel;

    //--------------------------------------------------------------------------------------------//

    public SpectrumView(Context context) {
        super(context);
        init();
    }

    public SpectrumView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(lineWidth);
        paint.setStyle(Paint.Style.FILL);
        paint.setDither(true);

        if (Build.VERSION.SDK_INT >= 29) {
            paint.setBlendMode(BlendMode.SCREEN);
        }
        paint.setAlpha(125);

        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    //--------------------------------------------------------------------------------------------//

    public void setFft(float[] fft) {
        this.fft = fft;
        invalidate();
    }

    public void setPaintColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    public void setPaintAlpha(float alpha) {
        paint.setAlpha((int) (255 * MathUtils.clamp(alpha, 0f, 1f)));
        invalidate();
    }

    public void setLinesWidth(float width) {
        lineWidth = width;
        paint.setStrokeWidth(width);
        invalidate();
    }

    public void setLineDistance(float space) {
        lineDistance = space;
        invalidate();
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

    private void computeLines(Canvas canvas) {
        lines = (int) (canvas.getWidth() / (lineWidth + lineDistance));
        peakLevel = new float[lines + 1];
        peakDownLevel = new float[lines + 1];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!Config.isUIAdvancedEffects()) {
            return;
        }

        if (lines == -1) {
            computeLines(canvas);
        }
        if (fft != null) {
            update(canvas);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        radius = Math.min(w, h) / 4;
        radius = Math.abs((int) (2 * radius * Math.sin(Math.PI / lines / 3)));
    }

    private void update(Canvas canvas) {
        int i = 1;
        while (i <= lines && i < fft.length) {

            float peak = fft[i] * peakRate;

            if (peak > peakLevel[i]) {
                peakLevel[i] = peak;
                peakDownLevel[i] = peakLevel[i] / peakDownRate;
            } else {
                peakLevel[i] = Math.max(peakLevel[i] - peakDownLevel[i], 0);
            }

            float lineX = (lineWidth + lineDistance) * i;
            float lineY = canvas.getHeight() - peakLevel[i];

            canvas.drawLine(lineX, canvas.getHeight(), lineX, lineY, paint);
            i++;
        }
    }
}
