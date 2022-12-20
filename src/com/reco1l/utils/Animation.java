package com.reco1l.utils;

// Created by Reco1l on 14/11/2022, 23:08

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewPropertyAnimator;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.Game;

import java.util.ArrayList;

public final class Animation {

    // Animation data
    private long duration = 300;
    private long delay = 0;

    private Float
            fromX,
            fromY,
            fromAlpha,
            fromScale,
            fromScaleX,
            fromScaleY,
            fromRotation;

    private Integer
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

    private Float
            toX,
            toY,
            toAlpha,
            toScale,
            toScaleX,
            toScaleY,
            toRotation;

    private Integer
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
    private Easing interpolator;

    private boolean
            fromPropertiesWithDelay = false,
            cancelCurrentAnimations = true,
            playWithLayer = false;
    // End

    // Listeners
    private Runnable
            runOnStart,
            runOnEnd;

    private UpdateListener runOnUpdate;
    // End

    private ArrayList<View> views;
    private ArrayList<ValueAnimator> valueAnimators;

    private boolean isViewAnimation = false;

    //--------------------------------------------------------------------------------------------//

    private Animation(View... views) {
        this.views = new ArrayList<>();
        isViewAnimation = true;

        for (View view : views) {
            if (view == null) {
                Log.i("Animation", "View is null, the animation will not apply properly!");
                continue;
            }
            this.views.add(view);
        }
    }

    private Animation(ValueAnimator valueAnimator) {
        valueAnimators = new ArrayList<>();
        valueAnimators.add(valueAnimator);
    }

    //--------------------------------------------------------------------------------------------//

    public static Animation of(View... view) {
        return new Animation(view);
    }

    public static Animation ofFloat(float from, float to) {
        return new Animation(ValueAnimator.ofFloat(from, to));
    }

    public static Animation ofInt(int from, int to) {
        return new Animation(ValueAnimator.ofInt(from, to));
    }

    public static Animation ofColor(int from, int to) {
        return new Animation(ValueAnimator.ofArgb(from, to));
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface UpdateListener {
        void onUpdate(Object value);
    }

    //--------------------------------------------------------------------------------------------//

    private void cancelViewAnimators() {
        int i = 0;
        while (i < views.size()) {
            View view = views.get(i);

            if (view != null) {
                view.animate().setListener(null);
                view.animate().cancel();
            }
            i++;
        }
    }

    private void cancelValueAnimations() {
        int i = 0;
        while (i < valueAnimators.size()) {
            ValueAnimator valueAnimator = valueAnimators.get(i);

            if (valueAnimator != null) {
                valueAnimator.removeAllListeners();
                valueAnimator.removeAllUpdateListeners();
                valueAnimator.cancel();
            }
            i++;
        }
    }

    public void cancel() {
        Game.activity.runOnUiThread(() -> {
            if (views != null) {
                cancelViewAnimators();
            }
            if (valueAnimators != null) {
                cancelValueAnimations();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void play() {
        play(duration);
    }

    public void play(long duration) {
        this.duration = duration;

        if (this.duration < 100) {
            this.duration = 100;
        }

        if (cancelCurrentAnimations) {
            cancel();
        }
        if (views != null && !views.isEmpty()) {
            handleViewAnimations();
            return;
        }
        if (valueAnimators != null && !valueAnimators.isEmpty()) {
            handleValueAnimations();
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void handleValueAnimations() {
        Game.activity.runOnUiThread(() -> {
            int i = 0;
            while (i < valueAnimators.size()) {
                ValueAnimator valueAnimator = valueAnimators.get(i);

                if (i == 0 && !isViewAnimation) {
                    valueAnimator.addListener(new BaseAnimationListener() {

                        public void onAnimationStart(Animator animation) {
                            if (runOnStart != null) {
                                runOnStart.run();
                            }
                        }

                        public void onAnimationEnd(Animator animation) {
                            if (runOnEnd != null) {
                                runOnEnd.run();
                            }

                            valueAnimator.removeAllListeners();
                            valueAnimator.removeAllUpdateListeners();
                        }
                    });

                    valueAnimator.addUpdateListener(animation -> {
                        if (runOnUpdate != null) {
                            runOnUpdate.onUpdate(animation.getAnimatedValue());
                        }
                    });
                }

                if (interpolator != null) {
                    valueAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
                }

                valueAnimator.setStartDelay(delay);
                valueAnimator.setDuration(duration);
                valueAnimator.start();
                i++;
            }
        });
    }

    private void handleViewAnimations() {
        Game.activity.runOnUiThread(() -> {
            int i = 0;
            while (i < views.size()) {

                View view = views.get(i);
                ViewPropertyAnimator viewAnimator = view.animate();

                handleFirstProperties(view);

                // Setting this to first view, otherwise will duplicate listeners
                if (i == 0) {
                    viewAnimator.setListener(new BaseAnimationListener() {
                        public void onAnimationStart(Animator animation) {
                            if (runOnStart != null) {
                                runOnStart.run();
                            }
                        }

                        public void onAnimationEnd(Animator animation) {
                            if (runOnEnd != null) {
                                runOnEnd.run();
                            }
                        }
                    });
                }

                if (interpolator != null) {
                    viewAnimator.setInterpolator(EasingHelper.asInterpolator(interpolator));
                }
                if (playWithLayer) {
                    viewAnimator.withLayer();
                }

                applyFinalProperties(viewAnimator);

                viewAnimator.setStartDelay(Math.max(delay, 0));
                viewAnimator.setDuration(duration);
                viewAnimator.start();
                i++;
            }
            handleValueAnimations();
        });
    }

    private void handleFirstProperties(View view) {
        if (fromPropertiesWithDelay && delay > 0) {
            view.postDelayed(() -> {
                applyInitialProperties(view);
                createParameterAnimations(view);
            }, delay - 1);
        } else {
            applyInitialProperties(view);
            createParameterAnimations(view);
        }
    }

    private void createParameterAnimations(View view) {
        if (valueAnimators == null) {
            valueAnimators = new ArrayList<>();
        }

        LayoutParams params = view.getLayoutParams();

        if (fromSize != null) {
            fromHeight = fromSize;
            fromWidth = fromSize;
        }
        if (toSize != null) {
            toHeight = toSize;
            toWidth = toSize;
        }

        if (toHeight != null) {
            if (fromHeight == null) {
                fromHeight = view.getHeight();
            }
            createParameterAnimator(view, params, fromHeight, toHeight, value ->
                    params.height = (int) value
            );
        }

        if (toWidth != null) {
            if (fromWidth == null) {
                fromWidth = view.getWidth();
            }
            createParameterAnimator(view, params, fromWidth, toWidth, value ->
                    params.width = (int) value
            );
        }

        MarginLayoutParams margins = (MarginLayoutParams) view.getLayoutParams();

        if (toMargins != null) {
            toVerticalMargins = toMargins;
            toHorizontalMargins = toMargins;
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
            createParameterAnimator(view, margins, fromTopMargin, toTopMargin, value ->
                    margins.topMargin = (int) value
            );
        }

        if (toBottomMargin != null) {
            if (fromBottomMargin == null) {
                fromBottomMargin = margins.bottomMargin;
            }
            createParameterAnimator(view, margins, fromBottomMargin, toBottomMargin, value ->
                    margins.bottomMargin = (int) value
            );
        }

        if (toLeftMargin != null) {
            if (fromLeftMargin == null) {
                fromLeftMargin = margins.leftMargin;
            }
            createParameterAnimator(view, margins, fromLeftMargin, toLeftMargin, value ->
                    margins.leftMargin = (int) value
            );
        }

        if (toRightMargin != null) {
            if (fromRightMargin == null) {
                fromRightMargin = margins.rightMargin;
            }
            createParameterAnimator(view, margins, fromRightMargin, toRightMargin, value ->
                    margins.rightMargin = (int) value
            );
        }
    }


    private void createParameterAnimator(View view, LayoutParams params, int from, int to, UpdateListener listener) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);

        animator.addUpdateListener(animation -> {
            listener.onUpdate((int) animation.getAnimatedValue());
            view.setLayoutParams(params);
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
        if (view == null) {
            return;
        }

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

        if (fromMargins != null) {
            fromVerticalMargins = fromMargins;
            fromHorizontalMargins = fromMargins;
        }
        if (fromVerticalMargins != null) {
            fromTopMargin = fromVerticalMargins;
            fromBottomMargin = fromVerticalMargins;
        }
        if (fromHorizontalMargins != null) {
            fromLeftMargin = fromHorizontalMargins;
            fromRightMargin = fromHorizontalMargins;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public Animation duration(long duration) {
        this.duration = duration;
        return this;
    }

    public Animation delay(long delay) {
        this.delay = delay;
        return this;
    }

    public Animation fromX(float fromX) {
        this.fromX = fromX;
        return this;
    }

    public Animation fromY(float fromY) {
        this.fromY = fromY;
        return this;
    }

    public Animation fromAlpha(float fromAlpha) {
        this.fromAlpha = fromAlpha;
        return this;
    }

    public Animation fromScale(float fromScale) {
        this.fromScale = fromScale;
        return this;
    }

    public Animation fromScaleX(float fromScaleX) {
        this.fromScaleX = fromScaleX;
        return this;
    }

    public Animation fromScaleY(float fromScaleY) {
        this.fromScaleY = fromScaleY;
        return this;
    }

    public Animation fromRotation(float fromRotation) {
        this.fromRotation = fromRotation;
        return this;
    }

    public Animation fromSize(int fromSize) {
        this.fromSize = fromSize;
        return this;
    }

    public Animation fromWidth(int fromWidth) {
        this.fromWidth = fromWidth;
        return this;
    }

    public Animation fromHeight(int fromHeight) {
        this.fromHeight = fromHeight;
        return this;
    }

    public Animation fromMargins(int fromMargins) {
        this.fromMargins = fromMargins;
        return this;
    }

    public Animation fromTopMargin(int fromTopMargin) {
        this.fromTopMargin = fromTopMargin;
        return this;
    }

    public Animation fromLeftMargin(int fromLeftMargin) {
        this.fromLeftMargin = fromLeftMargin;
        return this;
    }

    public Animation fromRightMargin(int fromRightMargin) {
        this.fromRightMargin = fromRightMargin;
        return this;
    }

    public Animation fromBottomMargin(int fromBottomMargin) {
        this.fromBottomMargin = fromBottomMargin;
        return this;
    }

    public Animation fromVerticalMargins(int fromVerticalMargins) {
        this.fromVerticalMargins = fromVerticalMargins;
        return this;
    }

    public Animation fromHorizontalMargins(int fromHorizontalMargins) {
        this.fromHorizontalMargins = fromHorizontalMargins;
        return this;
    }

    public Animation toX(float toX) {
        this.toX = toX;
        return this;
    }

    public Animation toY(float toY) {
        this.toY = toY;
        return this;
    }

    public Animation toAlpha(float toAlpha) {
        this.toAlpha = toAlpha;
        return this;
    }

    public Animation toScale(float toScale) {
        this.toScale = toScale;
        return this;
    }

    public Animation toScaleX(float toScaleX) {
        this.toScaleX = toScaleX;
        return this;
    }

    public Animation toScaleY(float toScaleY) {
        this.toScaleY = toScaleY;
        return this;
    }

    public Animation toRotation(float toRotation) {
        this.toRotation = toRotation;
        return this;
    }

    public Animation toSize(int toSize) {
        this.toSize = toSize;
        return this;
    }

    public Animation toWidth(int toWidth) {
        this.toWidth = toWidth;
        return this;
    }

    public Animation toHeight(int toHeight) {
        this.toHeight = toHeight;
        return this;
    }

    public Animation toMargins(int toMargins) {
        this.toMargins = toMargins;
        return this;
    }

    public Animation toTopMargin(int toTopMargin) {
        this.toTopMargin = toTopMargin;
        return this;
    }

    public Animation toLeftMargin(int toLeftMargin) {
        this.toLeftMargin = toLeftMargin;
        return this;
    }

    public Animation toRightMargin(int toRightMargin) {
        this.toRightMargin = toRightMargin;
        return this;
    }

    public Animation toBottomMargin(int toBottomMargin) {
        this.toBottomMargin = toBottomMargin;
        return this;
    }

    public Animation toVerticalMargins(int toVerticalMargins) {
        this.toVerticalMargins = toVerticalMargins;
        return this;
    }

    public Animation toHorizontalMargins(int toHorizontalMargins) {
        this.toHorizontalMargins = toHorizontalMargins;
        return this;
    }

    public Animation interpolator(Easing interpolator) {
        this.interpolator = interpolator;
        return this;
    }

    public Animation fromPropertiesWithDelay(boolean fromPropertiesWithDelay) {
        this.fromPropertiesWithDelay = fromPropertiesWithDelay;
        return this;
    }

    public Animation cancelCurrentAnimations(boolean cancelCurrentAnimations) {
        this.cancelCurrentAnimations = cancelCurrentAnimations;
        return this;
    }

    public Animation playWithLayer(boolean playWithLayer) {
        this.playWithLayer = playWithLayer;
        return this;
    }

    public Animation runOnStart(Runnable runOnStart) {
        this.runOnStart = runOnStart;
        return this;
    }

    public Animation runOnEnd(Runnable runOnEnd) {
        this.runOnEnd = runOnEnd;
        return this;
    }

    public Animation runOnUpdate(UpdateListener runOnUpdate) {
        this.runOnUpdate = runOnUpdate;
        return this;
    }
}
