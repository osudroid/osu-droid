package com.reco1l.utils;

import static android.view.MotionEvent.*;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.reco1l.Game;
import com.reco1l.ui.BaseFragment;
import com.reco1l.tables.Res;

import org.jetbrains.annotations.NotNull;

// Created by Reco1l on 23/6/22 20:44

public final class TouchHandler {

    private static final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    public TouchListener listener;

    private BaseFragment linkedFragment;

    private Handler handler;
    private Runnable longPress;
    private Vibrator vibrator;

    private boolean isLongPressActioned = false;

    //--------------------------------------------------------------------------------------------//

    public TouchHandler(TouchListener listener) {
        this.listener = listener;
        vibrator = (Vibrator) Game.activity.getSystemService(Context.VIBRATOR_SERVICE);
        handler = new Handler();

        longPress = () -> {
            isLongPressActioned = true;
            listener.onLongPress();
            if (vibrator != null) {
                vibrator.vibrate(50);
            }
        };
    }

    public TouchHandler(@NotNull Runnable onSingleTapUp) {
        listener = new TouchListener() {
            public void onPressUp() {
                onSingleTapUp.run();
            }
        };
    }

    //--------------------------------------------------------------------------------------------//

    private void handleEffects(View view, MotionEvent event) {

        switch (event.getAction()) {
            case ACTION_DOWN:
                view.setPressed(true);

                if (listener.getPressDownSound() != null) {
                    listener.getPressDownSound().play();
                }
                break;

            case ACTION_UP:
                view.setPressed(false);

                if (isLongPressActioned) {
                    if (listener.getLongPressSound() != null) {
                        listener.getLongPressSound().play();
                    }
                    break;
                }
                if (listener.getPressUpSound() != null) {
                    listener.getPressUpSound().play();
                }
                break;

            case ACTION_MOVE:
            case ACTION_CANCEL:
                view.setPressed(false);
                break;
        }
    }

    private void removeCallbacks() {
        if (handler != null) {
            handler.removeCallbacks(longPress);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void apply(View view) {
        if (listener == null) {
            return;
        }

        if (listener.useTouchEffect()) {
            handleRippleEffect(view);
        }

        view.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            notifyTouchEvent(event);
            handleEffects(view, event);

            if (action == ACTION_DOWN) {
                if (handler != null) {
                    handler.postDelayed(longPress, LONG_PRESS_TIMEOUT);
                }
                listener.onPressDown();
                return true;
            }

            if (action == ACTION_UP) {
                if (isLongPressActioned) {
                    isLongPressActioned = false;
                    return false;
                }
                removeCallbacks();

                if (listener.useOnlyOnce()) {
                    view.setOnTouchListener(null);
                }
                listener.onPressUp();
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

    public void linkToFragment(BaseFragment fragment) {
        linkedFragment = fragment;
    }

    //--------------------------------------------------------------------------------------------//

    private void handleRippleEffect(View view) {
        if (view.getForeground() != null) {
            return;
        }
        TypedValue outValue = new TypedValue();

        int id = android.R.attr.selectableItemBackground;
        if (listener.useBorderlessEffect()) {
            id = android.R.attr.selectableItemBackgroundBorderless;
        }

        Game.activity.getTheme().resolveAttribute(id, outValue, true);
        view.setForeground(Res.drw(outValue.resourceId));
    }
}
