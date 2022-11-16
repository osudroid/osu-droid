package com.reco1l.view;

// Created by Reco1l on 15/11/2022, 19:28

import static android.graphics.Shader.TileMode.*;

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.reco1l.utils.Animation2;

public class KiaiView extends View {

    private Animation2 fadeUp, fadeDown;

    private Paint leftPaint, rightPaint;

    private boolean
            isKiai = false,
            drawLeft = true,
            drawRight = true;

    private int
            color,
            cursor;
    //--------------------------------------------------------------------------------------------//

    public KiaiView(Context context) {
        super(context);
        init();
    }

    public KiaiView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    //--------------------------------------------------------------------------------------------//

    private void init() {
        color = Color.WHITE;

        leftPaint = new Paint();
        rightPaint = new Paint();

        leftPaint.setColor(color);
        rightPaint.setColor(color);

        leftPaint.setDither(true);
        rightPaint.setDither(true);

        if (Build.VERSION.SDK_INT >= 29) {
            leftPaint.setBlendMode(BlendMode.SCREEN);
            rightPaint.setBlendMode(BlendMode.SCREEN);
        }

        fadeUp = Animation2.ofInt(0, 255);
        fadeDown = Animation2.ofInt(255, 0);

        Animation2.UpdateListener<Object> listener = value -> {
            rightPaint.setAlpha((int) value);
            leftPaint.setAlpha((int) value);
        };

        fadeUp.runOnUpdate = listener;
        fadeDown.runOnUpdate = listener;
    }

    private void drawShaders(int color) {
        int transparent = Color.TRANSPARENT;
        int width = getWidth();

        LinearGradient lShader = new LinearGradient(0, 0, width / 4f, 0, color, transparent, CLAMP);
        leftPaint.setShader(lShader);

        LinearGradient rShader = new LinearGradient(width, 0, width - (width / 4f), 0, color, transparent, CLAMP);
        rightPaint.setShader(rShader);
    }

    //--------------------------------------------------------------------------------------------//

    public void setKiai(boolean isKiai) {
        this.isKiai = isKiai;
    }

    public void setPaintColor(int color) {
        this.color = color;
        drawShaders(color);
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        invalidate();
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        drawShaders(color);
    }

    public void onBeatUpdate(float beatLength, int beat) {
        long upTime = (long) (beatLength * 0.07f);
        long downTime = (long) (beatLength * 0.9f);

        if (!isKiai) {
            downTime *= 1.4f;
        }

        fadeUp.runOnStart = () -> {
            if (isKiai) {
                cursor = cursor == 0 ? 1 : 0;

                drawLeft = cursor == 0;
                drawRight = cursor == 1;
            } else {
                drawLeft = beat == 0;
                drawRight = beat == 0;
            }
        };

        fadeUp.play(upTime);
        fadeDown.delay = upTime;
        fadeDown.play(downTime);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        float rectWidth = width / 4f;

        if (drawLeft) {
            canvas.drawRect(0, 0, rectWidth, height, leftPaint);
        }
        if (drawRight) {
            canvas.drawRect(width - rectWidth, 0, width, height, rightPaint);
        }

        super.onDraw(canvas);
    }
}
