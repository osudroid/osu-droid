package com.reco1l.framework.input;

import static android.view.MotionEvent.*;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.reco1l.Game;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.tables.Res;

import org.jetbrains.annotations.NotNull;

import main.audio.BassSoundProvider;

// Created by Reco1l on 23/6/22 20:44

public final class TouchHandler {

    private static final long LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

    public TouchListener mListener;

    private final Handler mHandler;
    private final Vibrator mVibrator;
    private final Runnable mLongPressCallback;

    private BaseFragment mLinkedFragment;

    private boolean mIsLongPressActioned = false;

    //--------------------------------------------------------------------------------------------//

    public TouchHandler(TouchListener listener) {
        mListener = listener;
        mVibrator = (Vibrator) Game.activity.getSystemService(Context.VIBRATOR_SERVICE);
        mHandler = new Handler();

        mLongPressCallback = () -> {
            mIsLongPressActioned = true;
            listener.onLongPress();
            if (mVibrator != null) {
                mVibrator.vibrate(50);
            }
        };
    }

    public TouchHandler(@NotNull Runnable onUp) {
        this(new TouchListener() {
            public void onPressUp() {
                onUp.run();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void playSound(String sfx) {
        if (sfx == null) {
            return;
        }

        BassSoundProvider sound = Game.resourcesManager.getSound(sfx);
        if (sound != null) {
            sound.play();
        }
    }

    private void handleEffects(View view, MotionEvent event) {

        switch (event.getAction()) {
            case ACTION_DOWN:
                view.setPressed(true);
                playSound(mListener.getPressDownSound());
                break;

            case ACTION_UP:
                view.setPressed(false);

                if (mIsLongPressActioned) {
                    playSound(mListener.getLongPressSound());
                    break;
                }
                playSound(mListener.getPressUpSound());
                break;

            case ACTION_MOVE:
            case ACTION_CANCEL:
                view.setPressed(false);
                break;
        }
    }

    private void removeCallbacks() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mLongPressCallback);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void apply(View view) {
        if (mListener == null) {
            return;
        }

        if (mListener.useTouchEffect()) {
            handleRippleEffect(view);
        }

        view.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            notifyTouchEvent(event);
            handleEffects(view, event);

            mListener.setPosition(event.getRawX(), event.getRawY());

            if (action == ACTION_DOWN) {
                if (mHandler != null) {
                    mHandler.postDelayed(mLongPressCallback, LONG_PRESS_TIMEOUT);
                }
                mListener.onPressDown();
                return true;
            }

            if (action == ACTION_UP) {
                if (mIsLongPressActioned) {
                    mIsLongPressActioned = false;
                    return false;
                }
                removeCallbacks();

                if (mListener.useOnlyOnce()) {
                    view.setOnTouchListener(null);
                }
                mListener.onPressUp();
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
        if (mLinkedFragment != null) {
            mLinkedFragment.notifyTouchEvent(event.getAction());
        }
    }

    public void linkToFragment(BaseFragment fragment) {
        mLinkedFragment = fragment;
    }

    //--------------------------------------------------------------------------------------------//

    private void handleRippleEffect(View view) {
        if (view.getForeground() != null) {
            return;
        }
        TypedValue outValue = new TypedValue();

        int id = android.R.attr.selectableItemBackground;
        if (mListener.useBorderlessEffect()) {
            id = android.R.attr.selectableItemBackgroundBorderless;
        }

        Game.activity.getTheme().resolveAttribute(id, outValue, true);
        view.setForeground(Res.drw(outValue.resourceId));
    }
}
