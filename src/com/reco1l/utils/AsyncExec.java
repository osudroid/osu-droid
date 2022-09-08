package com.reco1l.utils;

// Created by Reco1l on 6/9/22 14:37

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Solution to the deprecated Android AsyncTask API.
 */
public abstract class AsyncExec {

    private final ExecutorService executor;
    private final Handler handler;

    public AsyncExec() {
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public abstract void run();
    public abstract void onComplete();

    public void execute() {
        this.executor.execute(() -> {
            this.run();
            this.handler.post(this::onComplete);
        });
    }

    public void cancel(boolean forceCancel) {
        if (forceCancel) {
            this.executor.shutdownNow();
        } else {
            this.executor.shutdown();
        }
        this.handler.removeCallbacks(this::onComplete);
    }
}
