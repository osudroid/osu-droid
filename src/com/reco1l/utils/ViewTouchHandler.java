package com.reco1l.utils;

import static android.view.MotionEvent.*;

import static com.reco1l.interfaces.IReferences.platform;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.listeners.TouchListener;

import org.jetbrains.annotations.NotNull;

// Created by Reco1l on 23/6/22 20:44

public class ViewTouchHandler {

    private static final int
            HIGHLIGHT_COLOR = 0x2FFFFFFF,
            DOWN_ANIM_DURATION = 100,
            UP_ANIM_DURATION = 100;

    private static final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    public TouchListener listener;

    private UIFragment linkedFragment;
    private ValueAnimator downAnim, upAnim;

    private Handler handler;
    private Runnable longPress;
    private Vibrator vibrator;

    private boolean isLongPressActioned = false;

    //--------------------------------------------------------------------------------------------//

    public ViewTouchHandler(TouchListener listener) {
        this.listener = listener;
        this.vibrator = (Vibrator) platform.context.getSystemService(Context.VIBRATOR_SERVICE);
        this.handler = new Handler();

        this.longPress = () -> {
            this.isLongPressActioned = true;
            this.listener.onLongPress();
            if (vibrator != null) {
                vibrator.vibrate(50);
            }
        };
    }

    public ViewTouchHandler(@NotNull Runnable onSingleTapUp) {
        this.listener = new TouchListener() {
            @Override
            public void onPressUp() {
                onSingleTapUp.run();
            }
        };
    }

    //--------------------------------------------------------------------------------------------//

    private void handleAnimation(MotionEvent event) {
        if (!listener.hasTouchEffect())
            return;

        switch (event.getAction()) {
            case ACTION_DOWN:
                if (downAnim != null) {
                    downAnim.start();
                }
                break;
            case ACTION_CANCEL:
            case ACTION_UP:
                if (upAnim != null) {
                    upAnim.start();
                }
                break;
        }
    }

    private void handleSfx() {
        if (listener.getClickSound() != null) {
            listener.getClickSound().play();
        }
    }

    private void removeCallbacks() {
        if (handler != null) {
            handler.removeCallbacks(longPress);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void apply(View view) {
        if (this.listener == null)
            return;

        if (this.listener.hasTouchEffect()) {
            createTouchEffect(view);
        }

        view.setOnTouchListener((v, event) -> {
            final int action = event.getAction();

            notifyTouchEvent(event);
            handleAnimation(event);

            if (action == ACTION_DOWN) {
                if (this.handler != null) {
                    this.handler.postDelayed(this.longPress, LONG_PRESS_TIMEOUT);
                }
                this.listener.onPressDown();
                return true;
            }

            if (action == ACTION_UP) {
                if (this.isLongPressActioned) {
                    this.isLongPressActioned = false;
                    return false;
                }
                removeCallbacks();
                handleSfx();

                if (this.listener.isOnlyOnce()) {
                    view.setOnTouchListener(null);
                }

                this.listener.onPressUp();
                return true;
            }

            if (action == ACTION_MOVE || action == ACTION_CANCEL) {
                removeCallbacks();
                return true;
            }
            return false;
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void notifyTouchEvent(MotionEvent event) {
        if (linkedFragment != null) {
            linkedFragment.onTouchEventNotified(event.getAction());
        }
    }

    public void linkToFragment(UIFragment fragment) {
        this.linkedFragment = fragment;
    }

    //--------------------------------------------------------------------------------------------//

    private GradientDrawable getViewForeground(View view) {
        if (Build.VERSION.SDK_INT < 24 || view == null)
            return null;

        if (view.getForeground() == null || !(view.getForeground() instanceof GradientDrawable)) {
            GradientDrawable drawable = new GradientDrawable();
            view.setForeground(drawable);
        }
        return (GradientDrawable) view.getForeground();
    }

    private float[] getViewCornerRadius(View view) {
        if (Build.VERSION.SDK_INT >= 24 && view.getBackground() != null) {

            // We can only get the view's corner radius if the background is a ShapeDrawable or a GradientDrawable.
            if (view.getBackground() instanceof GradientDrawable || view.getBackground() instanceof ShapeDrawable) {

                GradientDrawable drawable = (GradientDrawable) view.getBackground().mutate();
                float R = drawable.getCornerRadius();

                // 'android:radius' property will override any corner radius set in the XML.
                if (R > 0) {
                    float[] radii = {R, R, R, R, R, R, R, R};
                    drawable.setCornerRadii(radii);
                }
                return drawable.getCornerRadii();
            }
        }
        return new float[] {0, 0, 0, 0, 0, 0, 0, 0};
    }

    private void createTouchEffect(View view) {
        GradientDrawable shape = getViewForeground(view);

        // Unfortunately this doesn't work on API < 24
        if (Build.VERSION.SDK_INT < 24 || shape == null)
            return;

        downAnim = ValueAnimator.ofArgb(0, HIGHLIGHT_COLOR);
        downAnim.setDuration(DOWN_ANIM_DURATION);

        upAnim = ValueAnimator.ofArgb(HIGHLIGHT_COLOR, 0);
        upAnim.setDuration(UP_ANIM_DURATION);

        shape.setCornerRadii(getViewCornerRadius(view));

        AnimatorUpdateListener update = val -> {
            shape.setColor((int) val.getAnimatedValue());
            view.setForeground(shape);
        };

        downAnim.addUpdateListener(update);
        upAnim.addUpdateListener(update);
    }
}
