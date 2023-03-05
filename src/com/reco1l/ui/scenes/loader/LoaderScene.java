package com.reco1l.ui.scenes.loader;

// Created by Reco1l on 26/11/2022, 04:58

import com.reco1l.utils.execution.ITask;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.scenes.loader.fragments.LoaderFragment;
import com.reco1l.utils.execution.AsyncTask;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.ToastLogger;

public class LoaderScene extends BaseScene {

    public static final LoaderScene instance = new LoaderScene();

    private final LoaderFragment mFragment;

    private ITask mTask;

    private boolean mIsTaskCompleted = false;

    private int mProgress = -1;

    //--------------------------------------------------------------------------------------------//

    public LoaderScene() {
        super();

        mFragment = new LoaderFragment(this);
        mFragment.setMax(100);
    }

    //--------------------------------------------------------------------------------------------//

    public void async(ITask task) {
        if (task == null) {
            throw new RuntimeException("You can't pass a null task here!");
        }

        show();
        mTask = task;

        new AsyncTask() {
            public void run() {
                mTask.run();
            }

            public void onComplete() {
                mIsTaskCompleted = true;
            }
        }.execute();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onSceneChange(Scene lastScene, Scene newScene) {
        if (newScene == this) {
            ToastLogger.setPercentage(-1);
            mProgress = (int) ToastLogger.getPercentage();
        }
    }

    @Override
    protected void onSceneUpdate(float sec) {
        if (mFragment == null || !mFragment.isAdded()) {
            return;
        }

        mFragment.setProgress(mProgress);

        if (!mFragment.isConcurrentAnimation() && mIsTaskCompleted) {
            mIsTaskCompleted = false;

            if (mTask != null) {
                mFragment.close(mTask::onComplete);
            } else {
                mFragment.close();
            }
            mTask = null;
        }
    }

    @Override
    public boolean onBackPress() {
        return true;
    }

}
