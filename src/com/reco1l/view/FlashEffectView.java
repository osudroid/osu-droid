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

import com.reco1l.Game;
import com.reco1l.utils.Animation;

import ru.nsu.ccfit.zuev.osu.Config;

public class FlashEffectView extends View implements BaseView {

    private Animation fadeUp, fadeDown;

    private Paint leftPaint, rightPaint;

    private boolean
            isEnabled = true,
            drawLeft = true,
            drawRight = true;

    private int
            color,
            cursor;

    //--------------------------------------------------------------------------------------------//

    public FlashEffectView(Context context) {
        super(context);
        init();
    }

    public FlashEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlashEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FlashEffectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    //--------------------------------------------------------------------------------------------//

    private void init() {
        color = Color.WHITE;

        leftPaint = new Paint();
        rightPaint = new Paint();

        leftPaint.setColor(color);
        rightPaint.setColor(color);

        leftPaint.setDither(Config.isUseDither());
        rightPaint.setDither(Config.isUseDither());

        if (Build.VERSION.SDK_INT >= 29) {
            leftPaint.setBlendMode(BlendMode.SCREEN);
            rightPaint.setBlendMode(BlendMode.SCREEN);
        }

        Animation.UpdateListener listener = value -> {
            rightPaint.setAlpha((int) value);
            leftPaint.setAlpha((int) value);
        };

        fadeUp = Animation.ofInt(0, 255).runOnUpdate(listener);
        fadeDown = Animation.ofInt(255, 0).runOnUpdate(listener);
    }

    private void drawShaders(int color) {
        int transparent = Color.TRANSPARENT;
        int width = getWidth();

        LinearGradient lShader = new LinearGradient(0, 0, width / 4f, 0, color, transparent, CLAMP);
        leftPaint.setShader(lShader);

        LinearGradient rShader = new LinearGradient(width, 0, width - (width / 4f), 0, color, transparent, CLAMP);
        rightPaint.setShader(rShader);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        drawShaders(color);
    }

    //--------------------------------------------------------------------------------------------//

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

    public void onBeatUpdate() {
        float beatLength = Game.timingWrapper.getBeatLength();

        long upTime = (long) (beatLength * 0.07f);
        long downTime = (long) (beatLength * 0.9f);

        boolean isKiai = Game.timingWrapper.isKiai();
        if (!isKiai) {
            downTime *= 1.4f;
        }

        fadeUp.runOnStart(() -> {
                    if (isKiai) {
                        cursor = cursor == 0 ? 1 : 0;

                        drawLeft = cursor == 0;
                        drawRight = cursor == 1;
                    } else {
                        short beat = Game.timingWrapper.getBeat();

                        drawLeft = beat == 0;
                        drawRight = beat == 0;
                    }
                })
                .play(upTime);

        fadeDown.delay(upTime).play(downTime);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isEnabled)
            return;

        if (!isInEditMode()) {
            if (Game.timingWrapper.isNextBeat()) {
                onBeatUpdate();
            }
        }

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
        invalidate();
    }

    public void setEnabled(boolean bool) {
        isEnabled = bool;
    }
}
