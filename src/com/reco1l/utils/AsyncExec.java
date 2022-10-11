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

    private boolean isCompleted;

    private final Runnable onComplete = () -> {
        this.onComplete();
        this.isCompleted = true;
    };

    //--------------------------------------------------------------------------------------------//

    public AsyncExec() {
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    //--------------------------------------------------------------------------------------------//

    public abstract void run();
    public abstract void onComplete();

    public void onCancel(boolean wasForced) {}

    //--------------------------------------------------------------------------------------------//

    public final void execute() {
        this.executor.execute(() -> {
            this.isCompleted = false;
            this.run();
            this.handler.post(this.onComplete);
        });
    }

    public final void cancel(boolean force) {
        if (force) {
            this.executor.shutdownNow();
        } else {
            this.executor.shutdown();
        }
        this.handler.removeCallbacks(this.onComplete);
        this.onCancel(force);
    }

    //--------------------------------------------------------------------------------------------//

    public final boolean isCompleted() {
        return this.isCompleted;
    }

    public final boolean isCanceled() {
        return this.executor.isTerminated();
    }
}
