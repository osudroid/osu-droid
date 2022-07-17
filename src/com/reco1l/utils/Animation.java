package com.reco1l.utils;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.utils.interfaces.IMainClasses;

import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;

// Created by Reco1l on 23/6/22 20:44

/**
 * Simplifies the usage of ViewPropertyAnimator and ValueAnimator.
 * <p>Note: This is intended to be used in Views only.</p>
 */
public class Animation implements IMainClasses {

    private ViewPropertyAnimator anim;
    private ValueAnimator valueAnim;

    private final View view;

    private Easing interpolator;
    public Runnable onStart, onEnd;

    private long delay = 0;

    private float fromX, toX;
    private float fromY, toY;
    private float fromScaleX, toScaleX;
    private float fromScaleY, toScaleY;
    private float fromAlpha, toAlpha;
    private float fromRotation, toRotation;

    private boolean cancelPendingAnimations = true;

    private InterpolatorTo interpolatorTo = InterpolatorTo.BOTH;

    public enum InterpolatorTo {
        VIEW_PROPERTY_ANIMATOR,
        VALUE_ANIMATOR,
        BOTH
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * Don't forget to call {@link #play(long)} to start the animation
     *
     * @param view View to animate.
     */
    public Animation(View view) {
        this.view = view;
        if (view == null)
            return;

        // Initial values

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

    public Animation ofInt(int from, int to) {
        valueAnim = ValueAnimator.ofInt(from, to);
        return this;
    }

    public Animation ofFloat(float from, float to) {
        valueAnim = ValueAnimator.ofFloat(from, to);
        return this;
    }

    public Animation ofArgb(int from, int to) {
        valueAnim = ValueAnimator.ofArgb(from, to);
        return this;
    }

    public Animation runOnUpdate(AnimatorUpdateListener onUpdate) {
        valueAnim.addUpdateListener(onUpdate);
        return this;
    }

    public Animation size(float from, float to) {
        valueAnim = ValueAnimator.ofInt((int) from,(int) to);
        valueAnim.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        return this;
    }

    public Animation height(float from, float to) {
        valueAnim = ValueAnimator.ofInt((int) from,(int) to);
        valueAnim.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        return this;
    }

    public Animation width(float from, float to) {
        valueAnim = ValueAnimator.ofInt((int) from,(int) to);
        valueAnim.addUpdateListener(animation -> {
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        return this;
    }

    public Animation elevation(float from, float to) {
        valueAnim = ValueAnimator.ofFloat(from, to);
        valueAnim.addUpdateListener(animation ->
                view.setElevation((float) animation.getAnimatedValue()));
        return this;
    }

    public Animation cancelPending(boolean bool) {
        cancelPendingAnimations = bool;
        return this;
    }

    public Animation delay(long ms){
        delay = ms;
        return this;
    }

    public Animation pivot(Float X, Float Y) {
        if (view == null)
            return this;
        view.setPivotX(X);
        view.setPivotY(Y);
        return this;
    }

    public Animation scaleY(float from, float to) {
        fromScaleY = from;
        toScaleY = to;
        return this;
    }

    public Animation scaleX(float from, float to) {
        fromScaleX = from;
        toScaleX = to;
        return this;
    }

    /**
     * This method sets ScaleX and ScaleY at the same time.
     */
    public Animation scale(float from, float to) {
        fromScaleX = from;
        fromScaleY = from;
        toScaleX = to;
        toScaleY = to;
        return this;
    }

    public Animation moveX(float from, float to) {
        fromX = from;
        toX = to;
        return this;
    }

    public Animation moveY(float from, float to) {
        fromY = from;
        toY = to;
        return this;
    }

    public Animation interpolator(Easing interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public Animation interpolatorMode(InterpolatorTo mode) {
        interpolatorTo = mode;
        return this;
    }

    public Animation rotation(float from, float to) {
        fromRotation = from;
        toRotation = to;
        return this;
    }

    public Animation fade(float from, float to){
        fromAlpha = from;
        toAlpha = to;
        return this;
    }

    public Animation runOnStart(Runnable task) {
        this.onStart = task;
        return this;
    }

    public Animation runOnEnd(Runnable task) {
        onEnd = task;
        return this;
    }

    /**
     * @param duration duration of animation in milliseconds.
     */
    public void play(long duration) {
        if(view == null || !mActivity.hasWindowFocus()) {
            if (onStart != null || onEnd != null) {
                new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                    @Override
                    public void run() {
                        if (onStart != null)
                            mActivity.runOnUiThread(onStart);
                    }

                    @Override
                    public void onComplete() {
                        if (onEnd != null)
                            mActivity.runOnUiThread(onEnd);
                        anim = null;
                        valueAnim = null;
                    }
                });
            }
            return;
        }

        mActivity.runOnUiThread(() -> {

            if(cancelPendingAnimations)
                view.animate().cancel();

            anim = view.animate();

            // Translation X
            view.setTranslationX(fromX);
            anim.translationX(toX);

            // Translation Y
            view.setTranslationY(fromY);
            anim.translationY(toY);

            // Scale X
            view.setScaleX(fromScaleX);
            anim.scaleX(toScaleX);

            // Scale Y
            view.setScaleY(fromScaleY);
            anim.scaleY(toScaleY);

            // Alpha
            view.setAlpha(fromAlpha);
            anim.alpha(toAlpha);

            // Rotation
            view.setRotation(fromRotation);
            anim.rotation(toRotation);

            // Interpolator
            if (interpolator != null) {
                if (interpolatorTo == InterpolatorTo.VIEW_PROPERTY_ANIMATOR || interpolatorTo == InterpolatorTo.BOTH) {
                    anim.setInterpolator(EasingHelper.asInterpolator(interpolator));
                }
            }

            if (valueAnim != null) {
                valueAnim.setDuration(duration);
                valueAnim.setStartDelay(delay);
                if (interpolator != null) {
                    if (interpolatorTo == InterpolatorTo.VALUE_ANIMATOR || interpolatorTo == InterpolatorTo.BOTH) {
                        valueAnim.setInterpolator(EasingHelper.asInterpolator(interpolator));
                    }
                }
                valueAnim.start();
            }

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
                    valueAnim = null;
                }
            });

            anim.setStartDelay(delay);
            anim.setDuration(duration);
            anim.start();
        });
    }

    public ViewGroupAnimation forChildView(IChildViewAnimation childAnimation) {
        if (view == null)
            return null;

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            return new ViewGroupAnimation(viewGroup, childAnimation);
        }
        return null;
    }

    public interface IChildViewAnimation {
        Animation forChild(View child);
    }

    public static class ViewGroupAnimation {

        private final ViewGroup view;
        private final IChildViewAnimation childAnimation;

        private boolean countFromLast = false;
        private Runnable onStart, onEnd;
        private long delay = 0;

        public ViewGroupAnimation (ViewGroup view, IChildViewAnimation childAnimation) {
            this.view = view;
            this.childAnimation = childAnimation;
        }

        public ViewGroupAnimation delay(long ms) {
            delay = ms;
            return this;
        }

        public ViewGroupAnimation invertOrder(boolean countFromLast) {
            this.countFromLast = countFromLast;
            return this;
        }

        public ViewGroupAnimation runOnStart(Runnable task) {
            this.onStart = task;
            return this;
        }

        public ViewGroupAnimation runOnEnd(Runnable task) {
            onEnd = task;
            return this;
        }

        public void play(long duration) {
            if (view == null)
                return;

            view.postDelayed(() ->{
                int childCount = view.getChildCount();

                for (int i = countFromLast ? childCount - 1 : 0; countFromLast ? i >= 0 : i < childCount;) {
                    View child = view.getChildAt(i);
                    Animation anim = childAnimation.forChild(child);

                    if (countFromLast ? i == childCount - 1 : i == 0) {
                        anim.runOnStart(onStart);
                    }
                    if (countFromLast ? i == 0 : i == view.getChildCount() - 1) {
                        anim.runOnEnd(onEnd);
                    }

                    anim.delay(duration * i);
                    anim.play(duration);
                    i += countFromLast ? -1 : 1;
                }
            }, delay);
        }
    }
}
