package com.reco1l.framework.lang.execution;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Reco1l
 * @author ammar
 * Originally created by Reco1l, adapted by ammar.
 */
public final class Async
{

    private static final ExecutorService GLOBAL_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "async::")
    );

    public static void run(@NonNull Runnable task) {
        GLOBAL_EXECUTOR.execute(task);
    }

}
