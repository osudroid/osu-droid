package com.reco1l.utils.execution;

// Created by Reco1l on 16/11/2022, 22:57

import java.util.Timer;
import java.util.TimerTask;

public class ScheduledTask {

    public static void run(Runnable task, long delay) {
        if (delay <= 0) {
            if (task != null) {
                task.run();
            }
            return;
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (task != null) {
                    task.run();
                }
                cancel();
            }
        }, delay);
    }
}
