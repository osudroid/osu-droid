package com.reco1l.utils;

import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/6/22 20:44

public class ClickEffect {
    // Add a highlight effect to a view when it is clicked. Needed since Android doesn't
    // have a built-in highlight effect and Ripple effect seems not working.

    // It only works in API >= 24 since it uses view.setForeground() and getCornerRadius()
    // to keep view shape, devices with Android below Nougat will not show this effect.

    private static final int color = GlobalManager.getInstance().getMainActivity().getResources()
            .getColor(R.color.touchEffectHighlight);

    public ClickEffect(View view, MotionEvent event) {
        if (Build.VERSION.SDK_INT < 24 || view == null)
            return;

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(((GradientDrawable) view.getBackground()).getCornerRadius());

        ValueAnimator downAnim = ValueAnimator.ofArgb(0, color);
        ValueAnimator upAnim = ValueAnimator.ofArgb(color, 0);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downAnim.setDuration(100);
            downAnim.addUpdateListener(value -> {
                shape.setColor((int) value.getAnimatedValue());
                view.setForeground(shape);
            });
            downAnim.start();
        }

        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_OUTSIDE
                || event.getAction() == MotionEvent.ACTION_SCROLL
                || event.getAction() == MotionEvent.ACTION_MOVE) {

            upAnim.setDuration(300);
            upAnim.addUpdateListener(value -> {
                shape.setColor((int) value.getAnimatedValue());
                view.setForeground(shape);
            });
            upAnim.start();
        }

    }

}
