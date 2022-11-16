package com.reco1l.utils;

// Created by Reco1l on 14/11/2022, 23:08

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.Game;

import java.util.ArrayList;

public class Animation2 {

    // Animation data
    public long duration = 300;
    public Long delay;

    public Float
            fromX,
            fromY,
            fromAlpha,
            fromScale,
            fromScaleX,
            fromScaleY,
            fromRotation;

    public Integer
            fromSize,
            fromWidth,
            fromHeight,
            fromMargins,
            fromTopMargin,
            fromLeftMargin,
            fromRightMargin,
            fromBottomMargin,
            fromVerticalMargins,
            fromHorizontalMargins;

    public Float
            toX,
            toY,
            toAlpha,
            toScale,
            toScaleX,
            toScaleY,
            toRotation;

    public Integer
            toSize,
            toWidth,
            toHeight,
            toMargins,
            toTopMargin,
            toLeftMargin,
            toRightMargin,
            toBottomMargin,
            toVerticalMargins,
            toHorizontalMargins;
    // End

    // Behavior
    public Easing interpolator;

    public boolean
            cancelCurrentAnimations = true,
            playWithLayer = false;
    // End

    // Listeners
    public Runnable
            runOnStart,
            runOnEnd;

    public UpdateListener<Object> runOnUpdate;
    // End

    private View[] views;
    private ArrayList<ValueAnimator> valueAnimators;
    private ArrayList<ViewPropertyAnimator> viewAnimators;


    //--------------------------------------------------------------------------------------------//

    public Animation2(View... views) {
        this.views = views;
        viewAnimators = new ArrayList<>();

        for (View view : views) {
            viewAnimators.add(view.animate());
        }
    }

    public Animation2(ValueAnimator valueAnimator) {
        valueAnimators = new ArrayList<>();
        valueAnimators.add(valueAnimator);
    }

    //--------------------------------------------------------------------------------------------//

    public static Animation2 of(View... view) {
        return new Animation2(view);
    }

    public static Animation2 ofFloat(float from, float to) {
        return new Animation2(ValueAnimator.ofFloat(from, to));
    }

    public static Animation2 ofInt(int from, int to) {
        return new Animation2(ValueAnimator.ofInt(from, to));
    }

    public static Animation2 ofArgb(int from, int to) {
        return new Animation2(ValueAnimator.ofArgb(from, to));
    }

    //--------------------------------------------------------------------------------------------//

    private final BaseAnimationListener listener = new BaseAnimationListener() {

        public void onAnimationStart(Animator animation) {
            if (runOnStart != null) {
                runOnStart.run();
            }
        }

        public void onAnimationEnd(Animator animation) {
            if (valueAnimators != null) {
                removeListeners();
            }

            if (runOnEnd != null) {
                runOnEnd.run();
            }
        }
    };

    private void removeListeners() {
        if (viewAnimators != null) {
            for (ViewPropertyAnimator viewAnimator : viewAnimators) {
                viewAnimator.setListener(null);
            }
        }

        if (valueAnimators != null) {
            for (ValueAnimator valueAnimator : valueAnimators) {
                valueAnimator.removeAllListeners();
                valueAnimator.removeAllUpdateListeners();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface UpdateListener<T> {
        void onUpdate(T value);
    }

    @FunctionalInterface
    public interface BehaviorHandler {
        void onAnimate(Animation2 anim);
    }

    //--------------------------------------------------------------------------------------------//

    private void cancelViewAnimators() {

        int i = 0;
        while (i < viewAnimators.size()) {
            ViewPropertyAnimator viewAnimator = viewAnimators.get(i);

            if (viewAnimator != null) {
                viewAnimator.cancel();
            }
            i++;
        }
    }

    private void cancelValueAnimations() {
        int i = 0;
        while (i < valueAnimators.size()) {
            ValueAnimator valueAnimator = valueAnimators.get(i);

            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            i++;
        }
    }

    public void cancel() {
        removeListeners();

        if (viewAnimators != null) {
            cancelViewAnimators();
        }
        if (valueAnimators != null) {
            cancelValueAnimations();
        }
    }

    public void play() {
        play(duration);
    }

    public void play(long duration) {
        this.duration = duration;

        Game.mActivity.runOnUiThread(() -> {

            if (cancelCurrentAnimations) {
                cancel();
            }
            if (viewAnimators != null && !viewAnimators.isEmpty()) {
                handleViewAnimations();
                return;
            }
            if (valueAnimators != null && !valueAnimators.isEmpty()) {
                handleValueAnimations();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void handleValueAnimations() {
        if (valueAnimators == null) {
            return;
        }

        int i = 0;
        while (i < valueAnimators.size()) {
            ValueAnimator valueAnimator = valueAnimators.get(i);

            valueAnimator.removeAllListeners();
            if (i == 0) {
                valueAnimator.addListener(listener);

                valueAnimator.addUpdateListener(animation -> {
                    if (runOnUpdate != null) {
                        runOnUpdate.onUpdate(animation.getAnimatedValue());
                    }
                });
            }

            if (interpolator != null) {
                valueAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
            }

            if (delay != null) {
                valueAnimator.setStartDelay(delay);
            }
            valueAnimator.setDuration(duration);
            valueAnimator.start();
            i++;
        }
    }

    private void handleViewAnimations() {
        if (viewAnimators == null) {
            return;
        }

        int i = 0;
        while (i < viewAnimators.size()) {

            View view = views[i];
            ViewPropertyAnimator viewAnimator = viewAnimators.get(i);

            handleParameterAnimations(view);

            // Setting this to first view, otherwise will duplicate listeners
            if (i == 0) {
                viewAnimator.setListener(listener);
                viewAnimator.withStartAction(() -> applyInitialProperties(view));
            }

            if (interpolator != null) {
                viewAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
            }
            if (delay != null) {
                viewAnimator.setStartDelay(delay);
            }

            applyFinalProperties(viewAnimator);

            if (playWithLayer) {
                viewAnimator.withLayer();
            }

            viewAnimator.setDuration(duration);
            viewAnimator.start();
            i++;
        }
        // To play params animations (size, margins, etc)
        handleValueAnimations();
    }

    private void handleParameterAnimations(View view) {
        if (valueAnimators == null) {
            valueAnimators = new ArrayList<>();
        }
        valueAnimators.clear();

        LayoutParams params = view.getLayoutParams();

        // "toSize" will override toHeight and toWidth if you set it.
        if (toSize != null) {
            toHeight = (int) toSize;
            toWidth = (int) toSize;
        }

        if (toHeight != null) {
            if (fromHeight == null) {
                fromHeight = view.getHeight();
            }
            createParameterAnimator(fromHeight, toHeight, value -> params.height = value);
        }

        if (toWidth != null) {
            if (fromWidth == null) {
                fromWidth = view.getWidth();
            }
            createParameterAnimator(fromWidth, toWidth, value -> params.width = value);
        }

        MarginLayoutParams margins = (MarginLayoutParams) view.getLayoutParams();

        // "toMargins" will override toVerticalMargins and toHorizontal
        if (toMargins != null) {
            toVerticalMargins = (int) toMargins;
            toHorizontalMargins = (int) toMargins;
        }

        if (toVerticalMargins != null) {
            toTopMargin = toVerticalMargins;
            toBottomMargin = toVerticalMargins;
        }

        if (toHorizontalMargins != null) {
            toLeftMargin = toHorizontalMargins;
            toRightMargin = toHorizontalMargins;
        }

        if (toTopMargin != null) {
            if (fromTopMargin == null) {
                fromTopMargin = margins.topMargin;
            }
            createParameterAnimator(fromTopMargin, toTopMargin, value -> margins.topMargin = value);
        }

        if (toBottomMargin != null) {
            if (fromBottomMargin == null) {
                fromBottomMargin = margins.bottomMargin;
            }
            createParameterAnimator(fromBottomMargin, toBottomMargin, value -> margins.bottomMargin = value);
        }

        if (toLeftMargin != null) {
            if (fromLeftMargin == null) {
                fromLeftMargin = margins.leftMargin;
            }
            createParameterAnimator(fromLeftMargin, toLeftMargin, value -> margins.leftMargin = value);
        }

        if (toRightMargin != null) {
            if (fromRightMargin == null) {
                fromRightMargin = margins.rightMargin;
            }
            createParameterAnimator(fromRightMargin, toRightMargin, value -> margins.rightMargin = value);
        }
    }


    private void createParameterAnimator(int from, int to, UpdateListener<Integer> listener) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);

        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            listener.onUpdate(value);
        });

        valueAnimators.add(animator);
    }

    //--------------------------------------------------------------------------------------------//

    private void applyFinalProperties(ViewPropertyAnimator viewAnimator) {

        if (toX != null) {
            viewAnimator.translationX(toX);
        }
        if (toY != null) {
            viewAnimator.translationY(toY);
        }
        if (toAlpha != null) {
            viewAnimator.alpha(toAlpha);
        }
        if (toRotation != null) {
            viewAnimator.rotation(toRotation);
        }

        if (toScale != null) {
            toScaleX = toScale;
            toScaleY = toScale;
        }

        if (toScaleX != null) {
            viewAnimator.scaleX(toScaleX);
        }
        if (toScaleY != null) {
            viewAnimator.scaleY(toScaleY);
        }
    }

    private void applyInitialProperties(View view) {
        if (view != null) {

            if (fromX != null) {
                view.setTranslationX(fromX);
            }
            if (fromY != null) {
                view.setTranslationY(fromY);
            }
            if (fromAlpha != null) {
                view.setAlpha(fromAlpha);
            }
            if (fromRotation != null) {
                view.setRotation(fromRotation);
            }

            if (fromScale != null) {
                fromScaleX = fromScale;
                fromScaleY = fromScale;
            }

            if (fromScaleX != null) {
                view.setScaleX(fromScaleX);
            }
            if (fromScaleY != null) {
                view.setScaleY(fromScaleY);
            }

            if (fromSize != null) {
                fromWidth = fromSize;
                fromHeight = fromSize;
            }

            if (fromWidth != null) {
                ViewUtils.width(view, fromWidth);
            }
            if (fromHeight != null) {
                ViewUtils.height(view, fromHeight);
            }
        }
    }
}
