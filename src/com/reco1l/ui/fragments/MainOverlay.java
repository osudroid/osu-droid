package com.reco1l.ui.fragments;

import com.reco1l.global.Scenes;
import com.reco1l.ui.BaseFragment;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class MainOverlay extends BaseFragment {

    public static final MainOverlay instance = new MainOverlay();

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

    }
}
