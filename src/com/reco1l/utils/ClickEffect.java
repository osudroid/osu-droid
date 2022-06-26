package com.reco1l.utils;

import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/6/22 20:44

public class ClickEffect implements IMainClasses {
    // Add a highlight effect to a view when it is clicked. Needed since Android ripple effect
    // seems doesn't work properly.

    // It only works in API >= 24 since it uses view.setForeground() and getCornerRadius()
    // to keep view shape, devices with Android below Nougat will not show this effect.

    private static int color = 0;

    public ClickEffect(View view, MotionEvent event) {
        if (Build.VERSION.SDK_INT < 24 || view == null)
            return;

        if (color == 0)
            color = mActivity.getResources().getColor(R.color.touchEffectHighlight);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);

        // Only ShapeDrawable class can be casted to GradientDrawable.
        boolean allow = view.getBackground() instanceof GradientDrawable
                || view.getBackground() instanceof ShapeDrawable;

        if (view.getBackground() != null && allow)
            shape.setCornerRadius(((GradientDrawable) view.getBackground()).getCornerRadius());

        ValueAnimator downAnim = ValueAnimator.ofArgb(0, color);
        ValueAnimator upAnim = ValueAnimator.ofArgb(color, 0);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN: {
                downAnim.setDuration(100);
                downAnim.addUpdateListener(value -> {
                    shape.setColor((int) value.getAnimatedValue());
                    view.setForeground(shape);
                });
                downAnim.start();
                break;
            }

            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP: {
                upAnim.setDuration(300);
                upAnim.addUpdateListener(value -> {
                    shape.setColor((int) value.getAnimatedValue());
                    view.setForeground(shape);
                });
                upAnim.start();
                break;
            }
        }
    }

}
