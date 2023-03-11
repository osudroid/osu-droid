package com.reco1l.framework.execution;

// Created by Reco1l on 16/11/2022, 22:57

import java.util.Timer;
import java.util.TimerTask;

public abstract class ScheduledTask {

    private Timer mTimer;
    private TimerTask mCurrentTask;

    //--------------------------------------------------------------------------------------------//

    public ScheduledTask() {
        mTimer = new Timer();
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract void run();

    protected void onCancel() {
        // do something
    }

    //--------------------------------------------------------------------------------------------//

    public final void execute(long delay) {
        if (delay <= 0) {
            run();
            return;
        }

        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.purge();

        if (mCurrentTask != null) {
            mCurrentTask.cancel();
        }

        mCurrentTask = new TimerTask() {
            public void run() {
                ScheduledTask.this.run();
                cancel();
            }
        };

        mTimer.schedule(mCurrentTask, delay);
    }

    public final ScheduledTask cancel() {
        if (mCurrentTask != null) {
            mCurrentTask.cancel();
        }
        onCancel();
        return this;
    }

    // Ends timer thread, useful if you plan to reuse instances
    public final void free() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static void of(Runnable task, long delay) {
        if (task == null) {
            return;
        }

        new ScheduledTask() {
            public void run() {
                task.run();
                cancel();
                free();
            }
        }.execute(delay);
    }
}
