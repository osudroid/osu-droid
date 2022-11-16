package com.reco1l.utils;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.Game;
import com.reco1l.interfaces.IReferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// Created by Reco1l on 23/6/22 20:44

/**
 * Simplifies the usage of ViewPropertyAnimator and ValueAnimator.
 */
public class Animation {

    private final static long DEFAULT_DURATION = 1000;

    public Runnable onStart, onEnd;

    private View view;
    private Easing interpolator;
    private ViewPropertyAnimator anim;
    private List<ValueAnimator> valueAnimators;

    private float fromX, toX;
    private float fromY, toY;
    private float fromAlpha, toAlpha;
    private float fromScaleX, toScaleX;
    private float fromScaleY, toScaleY;
    private float fromRotation, toRotation;
    private long duration = -1, delay = 0;

    private boolean cancelPendingAnimations = true;
    private Interpolate interpolatorMode = Interpolate.BOTH;

    //--------------------------------------------------------------------------------------------//

    public enum Interpolate {
        PROPERTY_ANIMATOR,
        VALUE_ANIMATOR,
        BOTH
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * This constructor is intended to be used for multiple Animations.
     */
    public Animation() {
        valueAnimators = new ArrayList<>();
    }

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

        valueAnimators = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface IAnimationHandler {
        void onAnimate(Animation animation);
    }

    // ValueAnimator based animations
    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface ValueAnimationListener<T extends Number> {
        void onUpdate(T value);
    }

    public static class ValueAnimation<T extends Number> {

        private final ValueAnimator valueAnimator;
        private final Animation instance;

        //----------------------------------------------------------------------------------------//

        public ValueAnimation(ValueAnimator valueAnimator, Animation instance) {
            this.valueAnimator = valueAnimator;
            this.instance = instance;
        }

        //----------------------------------------------------------------------------------------//

        public ValueAnimation<T> interpolator(Easing interpolator) {
            valueAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
            return this;
        }

        @SuppressWarnings("unchecked")
        public Animation runOnUpdate(ValueAnimationListener<T> listener) {
            valueAnimator.removeAllUpdateListeners();
            valueAnimator.addUpdateListener(val -> {
                if (listener != null) {
                    listener.onUpdate((T) val.getAnimatedValue());
                }
            });
            return instance;
        }

        public Animation build() {
            return instance;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public ValueAnimation<Integer> ofInt(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimators.add(valueAnimator);
        return new ValueAnimation<>(valueAnimator, this);
    }

    public ValueAnimation<Float> ofFloat(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(from, to);
        valueAnimators.add(valueAnimator);
        return new ValueAnimation<>(valueAnimator, this);
    }

    public ValueAnimation<Integer> ofArgb(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(from, to);
        valueAnimators.add(valueAnimator);
        return new ValueAnimation<>(valueAnimator, this);
    }

    // Size
    //--------------------------------------------------------------------------------------------//

    public Animation size(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt((int) from, (int) to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    public Animation height(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt((int) from, (int) to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    public Animation width(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt((int) from, (int) to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    // Margins
    //--------------------------------------------------------------------------------------------//

    public Animation marginTop(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.topMargin = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    public Animation marginBottom(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.bottomMargin = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    public Animation marginLeft(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.leftMargin = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    public Animation marginRight(int from, int to) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(from, to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
            params.rightMargin = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    // Elevation
    //--------------------------------------------------------------------------------------------//

    public Animation elevation(float from, float to) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(from, to);
        valueAnimator.addUpdateListener(animation -> {
            if (view == null)
                return;
            view.setElevation((float) animation.getAnimatedValue());
        });
        valueAnimators.add(valueAnimator);
        return this;
    }

    // Scale
    //--------------------------------------------------------------------------------------------//

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

    // Translation
    //--------------------------------------------------------------------------------------------//

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

    // Interpolation
    //--------------------------------------------------------------------------------------------//

    public Animation interpolator(Easing interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public Animation interpolatorMode(Interpolate mode) {
        interpolatorMode = mode;
        return this;
    }

    // Rotation
    //--------------------------------------------------------------------------------------------//

    public Animation rotation(float from, float to) {
        fromRotation = from;
        toRotation = to;
        return this;
    }

    // Alpha
    //--------------------------------------------------------------------------------------------//

    public Animation fade(float from, float to) {
        fromAlpha = from;
        toAlpha = to;
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public Animation runOnStart(Runnable task) {
        onStart = task;
        return this;
    }

    public Animation runOnEnd(Runnable task) {
        onEnd = task;
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public Animation cancelPending(boolean bool) {
        cancelPendingAnimations = bool;
        return this;
    }

    public Animation delay(long ms) {
        delay = ms;
        return this;
    }

    public Animation duration(long ms) {
        duration = ms;
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public void play() {
        if (duration < 0) {
            duration = DEFAULT_DURATION;
        }
        play(duration);
    }

    /**
     * @param fDuration duration of animation in milliseconds.
     */
    public void play(final long fDuration) {

        if (view == null || !Game.mActivity.hasWindowFocus()) {

            if (valueAnimators != null) {
                for (ValueAnimator valueAnimator : valueAnimators) {
                    if (interpolator != null) {
                        if (interpolatorMode == Interpolate.VALUE_ANIMATOR || interpolatorMode == Interpolate.BOTH) {
                            valueAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
                        }
                    }
                    valueAnimator.setStartDelay(delay);
                    valueAnimator.setDuration(fDuration);

                    Game.mActivity.runOnUiThread(valueAnimator::start);
                }
            }

            ValueAnimator animator = ValueAnimator.ofFloat(0, 1f);

            animator.addListener(new BaseAnimationListener() {
                public void onAnimationStart(Animator animation) {
                    if (onStart != null) {
                        onStart.run();
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (onEnd != null) {
                        onEnd.run();
                    }
                }
            });
            animator.setStartDelay(delay);
            animator.setDuration(fDuration);

            Game.mActivity.runOnUiThread(animator::start);
            return;
        }

        Game.mActivity.runOnUiThread(() -> {
            if (cancelPendingAnimations) {
                view.animate().setListener(null);
                view.animate().cancel();
            }
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
                if (interpolatorMode == Interpolate.PROPERTY_ANIMATOR || interpolatorMode == Interpolate.BOTH) {
                    anim.setInterpolator(EasingHelper.asInterpolator(interpolator));
                }
            }

            if (valueAnimators != null) {
                for (ValueAnimator valueAnimator : valueAnimators) {
                    if (interpolator != null) {
                        if (interpolatorMode == Interpolate.VALUE_ANIMATOR || interpolatorMode == Interpolate.BOTH) {
                            valueAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
                        }
                    }
                    valueAnimator.setStartDelay(delay);
                    valueAnimator.setDuration(fDuration);
                    valueAnimator.start();
                }
            }

            anim.setListener(new BaseAnimationListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (onStart != null) {
                        onStart.run();
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onEnd != null) {
                        onEnd.run();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (onEnd != null) {
                        onEnd.run();
                    }
                }
            });

            anim.setStartDelay(delay);
            anim.setDuration(fDuration);
            anim.start();
        });
    }

    // Animation for layouts childs
    //--------------------------------------------------------------------------------------------//

    public ViewGroupAnimation forChildView(IChildViewAnimation childAnimation) {
        if (view == null)
            return null;

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            return new ViewGroupAnimation(viewGroup, childAnimation);
        }
        return null;
    }

    @FunctionalInterface
    public interface IChildViewAnimation {
        Animation forChild(View child);
    }

    public static class ViewGroupAnimation {

        private final ViewGroup view;
        private final IChildViewAnimation childAnimation;

        private boolean countFromLast = false;
        private Runnable onStart, onEnd;
        private long delay = 0;

        public ViewGroupAnimation(ViewGroup view, IChildViewAnimation childAnimation) {
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

            view.postDelayed(() -> {
                int childCount = view.getChildCount();
                int goneCount = 0;

                for (int i = countFromLast ? childCount - 1 : 0; countFromLast ? i >= 0 : i < childCount; ) {
                    View child = view.getChildAt(i);

                    if (child.getVisibility() == View.GONE) {
                        goneCount++;
                        i += countFromLast ? -1 : 1;
                        continue;
                    }

                    Animation anim = childAnimation.forChild(child);

                    if (countFromLast ? i == childCount - 1 : i == 0) {
                        anim.runOnStart(onStart);
                    }
                    if (countFromLast ? i == 0 : i == view.getChildCount() - 1) {
                        anim.runOnEnd(onEnd);
                    }

                    long delayPerChild = duration * (i - goneCount);

                    anim.delay(delayPerChild > 0 ? delayPerChild : 0);
                    anim.play(duration);
                    i += countFromLast ? -1 : 1;
                }
            }, delay);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public MultipleAnimation multi(Animation... animations) {
        return new MultipleAnimation(view, animations);
    }

    public static class MultipleAnimation {

        private final View view;

        private final Animation[] animations;
        private boolean sequential = false;
        private long delay = 0;

        //----------------------------------------------------------------------------------------//

        public MultipleAnimation(View view, Animation... animations) {
            this.animations = animations;
            this.view = view;
        }

        //----------------------------------------------------------------------------------------//

        public MultipleAnimation delay(long ms) {
            delay = ms;
            return this;
        }

        public MultipleAnimation sequential(boolean bool) {
            sequential = bool;
            return this;
        }

        public void play() {
            play(-1);
        }

        public void play(long duration) {
            if (animations.length == 0)
                return;
            Timer timer = new Timer();

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long delayCount = 0;

                    for (Animation anim : animations) {
                        if (anim.view == null && view != null) {
                            anim.view = view;
                            if (!sequential) {
                                anim.cancelPending(false);
                            }
                        }
                        if (duration > 0 && anim.duration <= 0) {
                            anim.duration = duration;
                        }
                        if (sequential) {
                            anim.delay += delayCount;
                            delayCount += anim.duration > 0 ? anim.duration : Animation.DEFAULT_DURATION;
                        }
                        anim.play();
                    }
                    timer.cancel();
                }
            }, delay);
        }
    }
}
