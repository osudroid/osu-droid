package com.reco1l.ui.scenes.loader.fragments;

import androidx.annotation.NonNull;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Animation;

import com.rimu.R;

public class LoaderFragment extends BaseFragment {

    private CircularProgressIndicator mIndicator;

    private boolean
            mShowAsOverlay = false,
            mAnimInProgress = false;

    //----------------------------------------------------------------------------------------//

    public LoaderFragment() {
        super();
    }

    public LoaderFragment(BaseScene... parents) {
        super(parents);
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

    @NonNull
    @Override
    protected Layers getLayer() {
        return mShowAsOverlay ? Layers.Overlay : Layers.Screen;
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

    //--------------------------------------------------------------------------------------------//

    // Useful to run a task when the close animation is over
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

    //--------------------------------------------------------------------------------------------//

    public boolean show(boolean showAsOverlay) {
        mShowAsOverlay = showAsOverlay;
        return super.show();
    }

    @Override
    public boolean show() {
        return show(false);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isConcurrentAnimation() {
        return mAnimInProgress;
    }

    //--------------------------------------------------------------------------------------------//

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

    //--------------------------------------------------------------------------------------------//
}
