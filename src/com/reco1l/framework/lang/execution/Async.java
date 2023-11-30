package com.reco1l.framework.lang.execution;

import androidx.annotation.NonNull;

import com.reco1l.framework.lang.Execution;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Reco1l
 */
public final class Async
{
    public static void run(@NonNull Runnable task)
    {
        Execution.async(() -> {
            task.run();
            return null;
        });
    }
}
