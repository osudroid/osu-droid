package com.reco1l.framework.lang.execution;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Reco1l
 */
public final class Async
{

    /**
     * This prefix is set to the thread name that's created by the Executor, this is useful to identify asynchronous threads.
     */
    public static final String THREAD_PREFIX = "async::";

    //----------------------------------------------------------------------------------------------------------------//

    private final ExecutorService mExecutor;

    //----------------------------------------------------------------------------------------------------------------//

    private Async(Runnable task)
    {
        mExecutor = Executors.newSingleThreadExecutor();

        mExecutor.execute(() -> {
            Thread.currentThread().setName(THREAD_PREFIX + Thread.currentThread().getName());

            task.run();
        });
    }

    //----------------------------------------------------------------------------------------------------------------//

    public static Async run(@NonNull Runnable task)
    {
        return new Async(task);
    }

    public ExecutorService getExecutor()
    {
        return mExecutor;
    }
}
