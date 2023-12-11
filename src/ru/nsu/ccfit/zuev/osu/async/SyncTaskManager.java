package ru.nsu.ccfit.zuev.osu.async;

import org.anddev.andengine.ui.activity.BaseGameActivity;

public class SyncTaskManager {

    private static final SyncTaskManager mgr = new SyncTaskManager();

    private BaseGameActivity activity;

    private SyncTaskManager() {

    }

    public static SyncTaskManager getInstance() {
        return mgr;
    }

    public void init(final BaseGameActivity act) {
        activity = act;
    }

    public void run(final Runnable runnable) {
        activity.runOnUpdateThread(runnable);
    }

}
