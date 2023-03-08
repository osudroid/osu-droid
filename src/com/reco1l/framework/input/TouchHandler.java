package com.reco1l.framework.input;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.MotionEvent.*;

import android.os.Handler;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.reco1l.Game;
import com.reco1l.management.resources.ResourceTable;
import com.reco1l.ui.base.BaseFragment;

import main.audio.BassSoundProvider;
import main.osu.MainActivity;

// Created by Reco1l on 23/6/22 20:44

public final class TouchHandler implements ResourceTable {

    private static final long LONG_PRESS_TIMEOUT;
    private static final Vibrator mVibrator;

    static {
        LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();

        mVibrator = (Vibrator) MainActivity.instance.getSystemService(VIBRATOR_SERVICE);
    }

    //--------------------------------------------------------------------------------------------//

    public TouchListener mListener;

    private final View mView;
    private final Handler mHandler;
    private final Runnable mLongPressCallback;

    private BaseFragment mLinkedFragment;

    private boolean mIsLongPressActioned = false;

    //--------------------------------------------------------------------------------------------//

    private TouchHandler(View view, TouchListener listener) {
        mView = view;
        mListener = listener;
        mHandler = new Handler();

        mLongPressCallback = () -> {
            mIsLongPressActioned = true;
            listener.onLongPress();

            if (mVibrator != null) {
                mVibrator.vibrate(50);
            }
        };

        bind();
    }

    public static TouchHandler of(View view, TouchListener listener) {
        return new TouchHandler(view, listener);
    }

    public static TouchHandler of(View view, Runnable onPressUp) {

        return new TouchHandler(view, new TouchListener() {

            public void onPressUp() {
                if (onPressUp != null) {
                    onPressUp.run();
                }
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

    public void bind() {
        if (mListener == null) {
            return;
        }

        if (mListener.useTouchEffect()) {
            handleRippleEffect(mView);
        }

        mView.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            notifyTouchEvent(event);
            handleEffects(mView, event);

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
        TypedValue outValue = attr(android.R.attr.selectableItemBackground, true);

        view.setForeground(drw(outValue.resourceId));
    }
}
