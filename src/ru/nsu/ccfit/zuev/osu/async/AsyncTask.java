package ru.nsu.ccfit.zuev.osu.async;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncTask {
    private final ExecutorService executor;
    private final Handler handler;

    private boolean isCompleted;

    private final Runnable mOnComplete = () -> {
        onComplete();
        isCompleted = true;
    };

    public AsyncTask() {
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public abstract void run();
    public void onComplete() {}

    public void onCancel(boolean wasForced) {}

    public final void execute() {
        executor.execute(() -> {
            isCompleted = false;
            Thread t = Thread.currentThread();
            t.setName("async::" + t.getName());
            run();
            handler.post(mOnComplete);
            executor.shutdown();
        });
    }

    public final void cancel(boolean force) {
        if (force) {
            executor.shutdownNow();
        } else {
            executor.shutdown();
        }
        handler.removeCallbacks(mOnComplete);
        onCancel(force);
    }

    public final boolean isCompleted() {
        return isCompleted;
    }

    public final boolean isTerminated() {
        return executor.isTerminated();
    }

    public final boolean isShutdown() {
        return executor.isShutdown();
    }
}