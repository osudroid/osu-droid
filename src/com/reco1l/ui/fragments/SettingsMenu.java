package com.reco1l.ui.fragments;

import static com.reco1l.management.Settings.*;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.edlplan.framework.easing.Easing;
import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.data.adapters.SettingsAdapter;
import com.reco1l.Game;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Animation;

import com.reco1l.utils.helpers.OnlineHelper;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 18/7/22 22:13

public final class SettingsMenu extends BaseFragment {

    public static final SettingsMenu instance = new SettingsMenu();

    private final SettingsAdapter mAdapter;

    private Sections mCurrent;
    private BouncyRecyclerView mRecyclerView;

    private View
            mBody,
            mLayer;

    //--------------------------------------------------------------------------------------------//

    private enum Sections {
        general(new General()),
        appearance(new Appearance()),
        gameplay(new Gameplay()),
        graphics(new Graphics()),
        sounds(new Sounds()),
        library(new Library()),
        advanced(new Advanced());

        private final Wrapper mWrapper;

        Sections(Wrapper wrapper) {
            mWrapper = wrapper;
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected boolean isOverlay() {
        return true;
    }

    @Override
    protected boolean isExtra() {
        return true;
    }

    @Override
    protected String getPrefix() {
        return "sm";
    }

    @Override
    protected int getLayout() {
        return R.layout.extra_settings_panel;
    }

    @Override
    public int getWidth() {
        return dimen(R.dimen.settingsPanelWidth);
    }

    //--------------------------------------------------------------------------------------------//

    public SettingsMenu() {
        super();

        ArrayList<Wrapper> mFragments = new ArrayList<>();

        for (Sections section : Sections.values()) {
            mFragments.add(section.mWrapper);
        }
        mAdapter = new SettingsAdapter(mFragments);

        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mCurrent = Sections.general;

        mRecyclerView = find("container");
        mLayer = find("layer");
        mBody = find("body");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Game.platform.animate(true, true)
                .toX(-50)
                .play(400);

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        Animation.of(mLayer)
                .fromX(getWidth())
                .toX(0)
                .interpolate(Easing.OutExpo)
                .play(350);

        Animation.of(mBody)
                .fromX(getWidth())
                .toX(0)
                .interpolate(Easing.OutExpo)
                .delay(50)
                .play(400);

        for (Sections tab : Sections.values()) {
            bindTouch(find(tab.name()), () -> navigateTo(tab));
        }
    }

    @Override
    protected void onPost() {
        mRecyclerView.post(() -> mRecyclerView.setAdapter(mAdapter));
    }

    private void navigateTo(Sections tab) {
        if (mCurrent == tab) {
            return;
        }
        mCurrent = tab;
        mRecyclerView.scrollToPosition(tab.ordinal());
    }

    //--------------------------------------------------------------------------------------------//

    private void applySettings() {
        Config.loadConfig(getContext());
        OnlineHelper.update();
        OnlineScoring.getInstance().login();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }
        unbindTouchHandlers();

        Game.platform.animate(true, true)
                .toX(0)
                .play(400);

        Animation.of(mBody)
                .toX(getWidth())
                .interpolate(Easing.InExpo)
                .runOnStart(() ->
                        Animation.of(rootBackground)
                                .toAlpha(0)
                                .play(300)
                )
                .play(350);

        Animation.of(mLayer)
                .toX(getWidth())
                .runOnEnd(() -> {
                    super.close();
                    applySettings();
                })
                .interpolate(Easing.InExpo)
                .delay(50)
                .play(400);
    }
}
