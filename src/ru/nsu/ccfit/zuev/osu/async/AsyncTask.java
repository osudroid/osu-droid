package ru.nsu.ccfit.zuev.osu.async;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncTask {

    private final ExecutorService executor;

    private final Handler handler;

    public AsyncTask() {
        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public abstract void run();

    public void onComplete() {
    }

    public final void execute() {
        executor.execute(() -> {
            Thread t = Thread.currentThread();
            t.setName("async::" + t.getName());
            run();
            handler.post(this::onComplete);
            executor.shutdown();
        });
    }

}