package com.reco1l.utils;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.reco1l.utils.interfaces.IMainClasses;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/6/22 20:44

public class ClickEffect implements IMainClasses {
    // Add a highlight effect to a view when it is clicked. Needed since Android ripple effect
    // seems doesn't work properly.

    // It only works in API >= 24 since it uses view.setForeground() and getCornerRadius()
    // to keep view shape, devices with Android below Nougat will not show this effect.

    private static int color = 0;

    private final View view;
    private ValueAnimator downAnim, upAnim;

    public ClickEffect(View view) {
        this.view = view;

        if (Build.VERSION.SDK_INT < 24 || view == null)
            return;

        if (color == 0)
            color = Res.color(R.color.touchEffectHighlight);

        downAnim = ValueAnimator.ofArgb(0, color);
        downAnim.setDuration(100);

        upAnim = ValueAnimator.ofArgb(color, 0);
        upAnim.setDuration(300);

        GradientDrawable shape = new GradientDrawable();

        // If the view has rounded corners we can only get the radius of those corners if the background
        // is a ShapeDrawable or a GradientDrawable.
        if (view.getBackground() != null) {
            if (view.getBackground() instanceof GradientDrawable || view.getBackground() instanceof ShapeDrawable) {

                GradientDrawable background = (GradientDrawable) view.getBackground().mutate();

                float R = background.getCornerRadius();
                // Setting 'android:radius' property will override any corner radius set in the XML.
                if (R > 0) {
                    float[] radii = {R, R, R, R, R, R, R, R};
                    background.setCornerRadii(radii);
                }
                shape.setCornerRadii(background.getCornerRadii());
            }
        }

        AnimatorUpdateListener update = val -> {
            shape.setColor((int) val.getAnimatedValue());
            view.setForeground(shape);
        };

        downAnim.addUpdateListener(update);
        upAnim.addUpdateListener(update);
    }


    public void play(MotionEvent event) {
        if (Build.VERSION.SDK_INT < 24 || view == null)
            return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downAnim.start();
                break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_SCROLL:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                upAnim.start();
                break;
        }
    }

}
