package com.reco1l.ui.fragments;

import static com.reco1l.management.Settings.*;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.base.BasePreferenceFragment;
import com.reco1l.ui.base.Layers;
import com.reco1l.framework.Animation;

import com.reco1l.utils.helpers.OnlineHelper;

import main.osu.Config;

import com.reco1l.framework.execution.Async;
import com.rimu.R;

// Created by Reco1l on 18/7/22 22:13

public final class SettingsMenu extends BaseFragment {

    public static final SettingsMenu instance = new SettingsMenu();

    private FrameLayout mPreferenceContainer;

    private View
            mBody,
            mLayer;

    private SettingsFragment mFragment;
    private Section mCurrent;

    //--------------------------------------------------------------------------------------------//

    private enum Section {
        general(new General()),
        appearance(new Appearance()),
        gameplay(new Gameplay()),
        graphics(new Graphics()),
        sounds(new Sounds()),
        library(new Library()),
        advanced(new Advanced());

        private final Wrapper wrapper;

        Section(Wrapper wrapper) {
            this.wrapper = wrapper;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public SettingsMenu() {
        super();
        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
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

    @Override
    protected void onLoad() {
        if (mFragment == null) {
            mFragment = new SettingsFragment();
        }

        mPreferenceContainer = find("container");
        mLayer = find("layer");
        mBody = find("body");

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

        mBody.setTranslationX(getWidth());

        Animation.of(mBody)
                 .toX(0)
                 .interpolate(Easing.OutExpo)
                 .delay(50)
                 .play(400);
    }

    @Override
    protected void onPost() {
        for (Section tab : Section.values()) {
            bindTouch(find(tab.name()), () -> navigateTo(tab));
        }

        Async.run(() -> mFragment.replace(mPreferenceContainer, getChildFragmentManager()));
    }

    private void navigateTo(Section tab) {
        mCurrent = tab;

        RecyclerView list = mFragment.getListView();
        if (list != null) {
            mFragment.scrollToPreference("pref_" + tab.name());
        }
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

    //--------------------------------------------------------------------------------------------//

    public static class SettingsFragment extends BasePreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            for (Section section : Section.values()) {
                addPreferencesFromResource(section.wrapper.getPreferenceXML());
            }
        }

        @Override
        protected void onLoad() {
            for (Section section : Section.values()) {
                section.wrapper.onLoad(this);
            }
        }
    }
}
