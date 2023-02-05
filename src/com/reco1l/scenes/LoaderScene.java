package com.reco1l.scenes;

// Created by Reco1l on 26/11/2022, 04:58

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.interfaces.ITask;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.execution.AsyncTask;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

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
    public void onShow() {
        ToastLogger.setPercentage(-1);
        mProgress = (int) ToastLogger.getPercentage();
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

    //--------------------------------------------------------------------------------------------//

    public static class LoaderFragment extends BaseFragment {

        private CircularProgressIndicator mIndicator;

        private boolean mAnimInProgress = false;

        //----------------------------------------------------------------------------------------//

        public LoaderFragment(BaseScene scene) {
            super(scene);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected String getPrefix() {
            return "ls";
        }

        @Override
        protected int getLayout() {
            return R.layout.layout_loader;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            mIndicator = find("progress");

            mAnimInProgress = true;
            Animation.of(mIndicator)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .runOnEnd(() -> mAnimInProgress = false)
                    .fromScale(0.8f)
                    .toScale(1)
                    .play(200);
        }

        public void close(Runnable task) {
            if (!isAdded()) {
                return;
            }
            mAnimInProgress = true;

            Animation.of(mIndicator)
                    .toAlpha(0)
                    .toScale(0.8f)
                    .runOnEnd(() -> {
                        mAnimInProgress = false;
                        super.close();

                        if (task != null) {
                            task.run();
                        }
                    })
                    .play(200);
        }

        @Override
        public void close() {
            close(null);
        }

        //----------------------------------------------------------------------------------------//

        public boolean isConcurrentAnimation() {
            return mAnimInProgress;
        }

        //----------------------------------------------------------------------------------------//

        public void setProgress(int progress) {
            if (mIndicator != null) {
                mIndicator.setIndeterminate(progress < 0);
                mIndicator.setProgress(progress);
            }
        }

        public void setMax(int max) {
            if (mIndicator != null) {
                mIndicator.setMax(max);
            }
        }
    }
}
