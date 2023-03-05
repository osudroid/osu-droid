package com.reco1l.ui.fragments;

import android.view.View;

import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.elements.FPSBadgeView;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class MainOverlay extends BaseFragment {

    public static final MainOverlay instance = new MainOverlay();

    private FPSBadgeView mFPSText;

    //--------------------------------------------------------------------------------------------//

    public MainOverlay() {
        super(Scenes.all());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.overlay_main;
    }

    @Override
    protected String getPrefix() {
        return "mo";
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mFPSText = find("fps");
    }

    @Override
    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
        if (!isLoaded()) {
            return;
        }

        if (newScene == Scenes.player) {
            mFPSText.setVisibility(View.GONE);
        } else {
            mFPSText.setVisibility(View.VISIBLE);
        }
    }
}
