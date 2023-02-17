package com.reco1l.utils.execution;

// Created by Reco1l on 6/9/22 14:37

import android.os.Handler;
import android.os.Looper;

import com.reco1l.interfaces.ITask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

 // Solution to the deprecated Android AsyncTask API.
public abstract class AsyncTask implements ITask {

    private final ExecutorService mExecutor;
    private final Handler mHandler;

    private boolean mIsCompleted;

    private final Runnable mOnComplete = () -> {
        this.onComplete();
        this.mIsCompleted = true;
    };

    //--------------------------------------------------------------------------------------------//

    public AsyncTask() {
        mExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(Looper.getMainLooper());
    }

    //--------------------------------------------------------------------------------------------//

    public abstract void run();
    public void onComplete() {}

    public void onCancel(boolean wasForced) {}

    //--------------------------------------------------------------------------------------------//

    public final void execute() {
        mExecutor.execute(() -> {
            mIsCompleted = false;
            Thread t = Thread.currentThread();
            t.setName("async::" + t.getName());
            run();
            mHandler.post(this.mOnComplete);
        });
    }

    public final void cancel(boolean force) {
        if (force) {
            mExecutor.shutdownNow();
        } else {
            mExecutor.shutdown();
        }
        mHandler.removeCallbacks(this.mOnComplete);
        onCancel(force);
    }

    //--------------------------------------------------------------------------------------------//

    public final boolean isCompleted() {
        return mIsCompleted;
    }

    public final boolean isTerminated() {
        return mExecutor.isTerminated();
    }

    public final boolean isShutdown() {
        return mExecutor.isShutdown();
    }
}
