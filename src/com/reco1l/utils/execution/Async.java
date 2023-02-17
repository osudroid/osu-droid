package com.reco1l.utils.execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Async {

    private final ExecutorService mExecutor;

    //--------------------------------------------------------------------------------------------//

    private Async(Runnable task) {
        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.execute(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("async::" + thread.getName());

            task.run();
        });
    }

    //--------------------------------------------------------------------------------------------//

    public static Async run(Runnable task) {
        return new Async(task);
    }

    public ExecutorService getExecutor() {
        return mExecutor;
    }
}
