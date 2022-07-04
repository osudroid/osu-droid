package com.reco1l.utils;

import android.view.View;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.utils.interfaces.IMainClasses;

// Created by Reco1l on 23/6/22 20:44

/**
 * Simplifies the usage of ViewPropertyAnimator class.
 */
public class Animator implements IMainClasses {

    private static ViewPropertyAnimator anim;

    private final View view;

    private Easing interpolator;
    public Runnable onStart, onEnd;

    private long delay = 0;

    private float pivotX, pivotY;
    private float fromX, toX;
    private float fromY, toY;
    private float fromScaleX, toScaleX;
    private float fromScaleY, toScaleY;
    private float fromAlpha, toAlpha;

    //--------------------------------------------------------------------------------------------//

    /**
     * Don't forget to call {@link #play(long)} to start the animation
     *
     * @param view View to animate.
     */
    public Animator(View view) {
        this.view = view;
        if (view == null)
            return;

        // Initial values
        pivotX = view.getPivotX();
        pivotY = view.getPivotY();

        fromX = view.getTranslationX();
        toX = fromX;
        fromY = view.getTranslationY();
        toY = fromY;

        fromScaleX = view.getScaleX();
        toScaleX = fromScaleX;
        fromScaleY = view.getScaleY();
        toScaleY = fromScaleY;

        fromAlpha = view.getAlpha();
        toAlpha = fromAlpha;
    }

    public Animator delay(long ms){
        delay = ms;
        return this;
    }

    public Animator pivot(Float X, Float Y) {
        pivotX = X;
        pivotY = Y;
        return this;
    }

    public Animator scaleY(float from, float to) {
        fromScaleY = from;
        toScaleY = to;
        return this;
    }

    public Animator scaleX(float from, float to) {
        fromScaleX = from;
        toScaleX = to;
        return this;
    }

    /**
     * This method sets ScaleX and ScaleY at the same time.
     */
    public Animator scale(float from, float to) {
        fromScaleX = from;
        fromScaleY = from;
        toScaleX = to;
        toScaleY = to;
        return this;
    }

    public Animator moveX(float from, float to) {
        fromX = from;
        toX = to;
        return this;
    }

    public Animator moveY(float from, float to) {
        fromY = from;
        toY = to;
        return this;
    }

    /**
     * @param interpolator The interpolator to use.
     */
    public Animator interpolator(Easing interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public Animator fade(float from, float to){
        fromAlpha = from;
        toAlpha = to;
        return this;
    }

    public Animator runOnStart(Runnable task) {
        this.onStart = task;
        return this;
    }

    public Animator runOnEnd(Runnable task) {
        onEnd = task;
        return this;
    }

    /**
     * @param duration duration of animation in milliseconds.
     */
    public void play(long duration) {
        if(view == null || !mActivity.hasWindowFocus())
          return;

        mActivity.runOnUiThread(() -> {
            view.animate().cancel();

            anim = view.animate();

            // Translation X
            view.setTranslationX(fromX);
            anim.translationX(toX);

            // Translation Y
            view.setTranslationY(fromY);
            anim.translationY(toY);

            // Pivot
            view.setPivotX(pivotX);
            view.setPivotY(pivotY);

            // Scale X
            view.setScaleX(fromScaleX);
            anim.scaleX(toScaleX);

            // Scale Y
            view.setScaleY(fromScaleY);
            anim.scaleY(toScaleY);

            // Alpha
            view.setAlpha(fromAlpha);
            anim.alpha(toAlpha);

            // Interpolator
            if (interpolator != null)
                anim.setInterpolator(EasingHelper.asInterpolator(interpolator));

            anim.setListener(new BaseAnimationListener() {
                @Override
                public void onAnimationStart(android.animation.Animator animation) {
                    if (onStart != null)
                        onStart.run();
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (onEnd != null)
                        onEnd.run();
                    super.onAnimationEnd(animation);
                    anim = null;
                }
            });

            anim.setStartDelay(delay);
            anim.setDuration(duration);
            anim.start();
        });
    }

}
