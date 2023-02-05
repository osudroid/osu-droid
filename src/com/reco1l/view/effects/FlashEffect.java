package com.reco1l.view.effects;

// Created by Reco1l on 15/11/2022, 19:28

import static android.graphics.Shader.TileMode.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.utils.Animation;
import com.reco1l.view.BaseView;

import ru.nsu.ccfit.zuev.osu.Config;

public class FlashEffect extends View implements BaseView {

    private Paint paint;
    private Animation fadeUp, fadeDown;

    private boolean
            drawLeft = true,
            drawRight = true;

    private int
            color,
            cursor;

    private float rectWidth;

    //--------------------------------------------------------------------------------------------//

    public FlashEffect(Context context) {
        this(context, null);
    }

    public FlashEffect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate(AttributeSet attrs) {
        color = Color.WHITE;

        paint = new Paint();
        paint.setDither(Config.isUseDither());

        Animation.UpdateListener listener = value -> paint.setAlpha((int) value);

        fadeUp = Animation.ofInt(0, 255).runOnUpdate(listener);
        fadeDown = Animation.ofInt(255, 0).runOnUpdate(listener);
    }

    //--------------------------------------------------------------------------------------------//

    private void handleColor(boolean dark) {
        if (dark && color == Color.WHITE || !dark && color == Color.BLACK) {
            return;
        }
        color = dark ? Color.WHITE : Color.BLACK;
        createShader();
    }

    private void createShader() {
        LinearGradient shader = new LinearGradient(0, 0, rectWidth, 0, color, Color.TRANSPARENT, CLAMP);
        paint.setShader(shader);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rectWidth = w / 5f;
        createShader();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        if (Game.timingWrapper.isNextBeat()) {
            onBeatUpdate();
        }
        handleColor(UI.background.isDark());

        int h = canvas.getHeight();
        int w = canvas.getWidth();

        if (drawLeft) {
            canvas.drawRect(0, 0, rectWidth, h, paint);
        }
        if (drawRight) {
            canvas.save();
            canvas.rotate(180, w / 2f, h / 2f);
            canvas.drawRect(0, 0, rectWidth, h, paint);
            canvas.restore();
        }

        super.onDraw(canvas);
        invalidate();

        if (Game.songService != null) {
            setAlpha(Game.songService.getLevel());
        }
    }

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

                        // 3 equals 4th beat
                        drawLeft = beat == 3;
                        drawRight = beat == 3;
                    }
                })
                .play(upTime);

        fadeDown.delay(upTime).play(downTime);
    }
}
