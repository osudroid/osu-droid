package com.reco1l.view;
// Created by Reco1l on 18/11/2022, 23:01

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.edlplan.framework.easing.Easing;
import com.reco1l.utils.Animation;

public class ExpandEffectView extends View {

    private Paint backPaint, frontPaint;

    private float
            minRadius,
            backRadius,
            frontRadius;

    private boolean isFadeEnabled = true;

    //--------------------------------------------------------------------------------------------//

    public ExpandEffectView(Context context) {
        super(context);
        init();
    }

    public ExpandEffectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        //setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        backPaint = new Paint();
        backPaint.setColor(Color.WHITE);
        backPaint.setAntiAlias(true);

        frontPaint = new Paint();
        frontPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        frontPaint.setAntiAlias(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (backRadius <= getMaxRadius()) {
            canvas.drawCircle(width / 2f, height / 2f, backRadius, backPaint);
        }

        if (frontRadius <= getMaxRadius()) {
            canvas.drawCircle(width / 2f, height / 2f, frontRadius, frontPaint);
        }
        super.onDraw(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    public void setMinRadius(int min) {
        minRadius = min;
    }

    public void setPaintColor(int color) {
        backPaint.setColor(color);
    }

    public void setFadeEffect(boolean bool) {
        isFadeEnabled = bool;
    }

    //--------------------------------------------------------------------------------------------//

    public float getMaxRadius() {
        if (getHeight() <= getWidth()) {
            return getHeight() / 2f;
        }
        return getWidth() / 2f;
    }

    public void play(int ms) {
        backRadius = minRadius;
        frontRadius = minRadius;

        Animation.ofFloat(minRadius, getMaxRadius())
                .runOnUpdate(value -> {
                    backRadius = (float) value;
                    invalidate();
                })
                .play(ms);

        Animation.ofFloat(0, getMaxRadius())
                .runOnUpdate(value -> {
                    frontRadius = (float) value;
                    invalidate();
                })
                .interpolator(Easing.OutExpo)
                .play(ms);

        if (isFadeEnabled) {
            Animation.ofInt(255, 0)
                    .runOnUpdate(value -> backPaint.setAlpha((int) value))
                    .interpolator(Easing.OutExpo)
                    .play(ms);
        }

    }
}
