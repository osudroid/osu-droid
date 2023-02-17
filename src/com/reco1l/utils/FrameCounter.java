package com.reco1l.utils;

import android.view.Choreographer;

// Counts FPS from native UI using choreographer callback
public final class FrameCounter {

    private static float
            mFPS,
            mFrameTime;

    private static long
            mElapsedTime,
            mLastTime;

    private static int mFrameCount;

    private static Choreographer mChoreographer;

    private static boolean mCountFromEngine = false;

    //--------------------------------------------------------------------------------------------//

    static {
        Logging.initOf(FrameCounter.class);
    }

    private FrameCounter() {
    }

    //--------------------------------------------------------------------------------------------//

    public static void start() {
        mChoreographer = Choreographer.getInstance();
        mChoreographer.postFrameCallback(ns -> handleNextFrame());
    }

    private static void handleNextFrame() {
        mFrameCount++;

        long time = System.currentTimeMillis();
        long span = time - mLastTime;

        mElapsedTime += span;
        mLastTime = time;

        if (!mCountFromEngine) {
            mFPS = mFrameCount / (mElapsedTime / 1000f);
            mFrameTime = span;
        }
        mChoreographer.postFrameCallback(ns -> handleNextFrame());

        if (mElapsedTime > 1000) {
            mFrameCount = 0;
            mElapsedTime = 0;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static float getFPS() {
        return mFPS;
    }

    public static float getFrameTime() {
        return mFrameTime;
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized static void countFromEngine(boolean countFromEngine) {
        mCountFromEngine = countFromEngine;
    }

    public synchronized static void setFPS(float fps) {
        mFPS = fps;
    }

    public synchronized static void setFrameTime(float ft) {
        mFrameTime = ft;
    }
}
