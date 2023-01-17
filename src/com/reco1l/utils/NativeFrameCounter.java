package com.reco1l.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;

import com.reco1l.Game;

// Counts FPS from native UI using choreographer
public final class NativeFrameCounter {

    private static float mFPS;

    private static long
            mLastTime,
            mFrameTime;

    private static Handler mHandler;

    //--------------------------------------------------------------------------------------------//

    private NativeFrameCounter() {}

    //--------------------------------------------------------------------------------------------//

    public static void startCounter() {
        if (mHandler != null) {
            throw new RuntimeException("NativeFrameCounter was already started previously!");
        }
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(NativeFrameCounter::handleNextFrame);
    }

    private static void handleNextFrame() {
        Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
            long current = System.currentTimeMillis();

            mFrameTime = current - mLastTime;
            mLastTime = current;

            mFPS = Math.min((float) 1000.0 / mFrameTime, Game.activity.getRefreshRate());
            handleNextFrame();
        });
    }

    //--------------------------------------------------------------------------------------------//

    public static float getFPS() {
        return mFPS;
    }

    public static float getFrameTime() {
        return mFrameTime;
    }
}
