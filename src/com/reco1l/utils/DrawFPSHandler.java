package com.reco1l.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import com.reco1l.Game;

public class DrawFPSHandler {

    private static float fps;

    private static long
            lastTime,
            frameTime;


    public static void startCounter() {
        new Handler(Looper.getMainLooper()).post(DrawFPSHandler::handleNextFrame);
    }

    private static void handleNextFrame() {
        Choreographer.getInstance().postFrameCallback(frameTimeNanos -> {
            long current = System.currentTimeMillis();

            frameTime = current - lastTime;
            lastTime = current;

            fps = Math.min((float) 1000.0 / frameTime, Game.activity.getRefreshRate());
            handleNextFrame();
        });
    }

    public static float getFPS() {
        return fps;
    }

    public static float getFrameTime() {
        return frameTime;
    }
}
