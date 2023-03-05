package com.reco1l.ui.scenes.selector.fragments;

// Created by Reco1l on 20/12/2022, 05:40

import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.factor.bouncy.BouncyRecyclerView;

import com.reco1l.Game;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.UI;
import com.reco1l.data.adapters.ModCustomizationAdapter;
import com.reco1l.annotation.Size;
import com.reco1l.management.modding.ModAcronyms;
import com.reco1l.tables.Res;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Views;
import com.reco1l.view.IconButtonView;

import com.reco1l.management.modding.mods.CustomDifficultyMod;
import com.reco1l.management.modding.mods.CustomSpeedMod;
import com.reco1l.management.modding.mods.FlashlightMod;
import com.reco1l.management.modding.mods.LegacyModWrapper;
import com.reco1l.management.modding.mods.ModWrapper;
import com.reco1l.data.adapters.ModSectionAdapter;
import com.reco1l.ui.elements.ModBadgeView;

import java.util.ArrayList;

import main.osu.game.mods.GameMod;
import com.rimu.R;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

public final class ModMenu extends BaseFragment implements ModAcronyms {

    public static final ModMenu instance = new ModMenu();

    private final IconButtonView button = Scenes.selector.modsButton;

    @SuppressWarnings("FieldCanBeLocal")
    private Section
            mIncrease,
            mReduction,
            mAutomation,
            mMiscellaneous;

    private ModCustomizationAdapter mCustomizationAdapter;
    private ModSectionAdapter mSectionAdapter;
    private BouncyRecyclerView mCustomization;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.selector_mod_menu;
    }

    @Override
    protected String getPrefix() {
        return "mm";
    }

    @Override
    protected boolean isExtra() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    private void loadMods() {
        Section[] sections = {
                mIncrease = new Section("Difficulty Increase"),
                mReduction = new Section("Difficulty Reduction"),
                mAutomation = new Section("Automation"),
                mMiscellaneous = new Section("Miscellaneous")
        };

        for (int i = 0; i < GameMod.values().length; ++i) {
            GameMod mod = GameMod.values()[i];
            Section section = mMiscellaneous;

            if (mod.equals(EZ, NF, HT, REZ)) {
                section = mReduction;
            }
            if (mod.equals(HR, DT, NC, HD, PR, SD, SC, PF, FL)) {
                section = mIncrease;

                if (mod.equals(FL)) {
                    section.add(new FlashlightMod());
                    continue;
                }
            }
            if (mod.equals(RX, AU, AP)) {
                section = mAutomation;
            }
            section.add(new LegacyModWrapper(mod));
        }
        loadCustomMods();

        mSectionAdapter = new ModSectionAdapter(sections);
        mCustomizationAdapter = new ModCustomizationAdapter(new ArrayList<>());
    }

    private void loadCustomMods() {
        mMiscellaneous.add(new CustomDifficultyMod());
        mMiscellaneous.add(new CustomSpeedMod());
    }

    @Override
    protected void onLoad() {
        if (mSectionAdapter == null) {
            loadMods();
        }

        RecyclerView sectionList = find("sectionContainer");
        sectionList.setLayoutManager(new LinearLayoutManager(getContext(), HORIZONTAL, false));
        sectionList.setAdapter(mSectionAdapter);

        mCustomization = find("customRv");
        mCustomization.setLayoutManager(new LinearLayoutManager(getContext(), HORIZONTAL, false));
        mCustomization.setAdapter(mCustomizationAdapter);

        bindTouch(find("clear"), this::clear);
        bindTouch(find("custom"), this::switchCustomizationVisibility);
    }

    //--------------------------------------------------------------------------------------------//

    public void onModSelect(ModWrapper pWrapper, boolean pByFlag) {
        if (Game.modManager.remove(pWrapper)) {
            pWrapper.onSelect(false);
        } else if (Game.modManager.add(pWrapper) && !pByFlag) {
            pWrapper.onSelect(true);
        }
        updateCustomizations();
        updateButtonWidget();
    }

    //--------------------------------------------------------------------------------------------//

    private void clear() {
        for (ModWrapper mod : Game.modManager.getList()) {
            mod.holder.onDeselect();
        }
        Game.modManager.clear();

        updateCustomizations();
        updateButtonWidget();
    }

    private void switchCustomizationVisibility() {
        if (mCustomization.getVisibility() != View.VISIBLE) {
            mCustomization.setVisibility(View.VISIBLE);
        } else {
            mCustomization.setVisibility(View.GONE);
        }
    }

    private void updateCustomizations() {
        mCustomizationAdapter.clear();

        for (ModWrapper wrapper : Game.modManager.getList()) {
            if (wrapper.getProperties() != null) {
                mCustomizationAdapter.getData().add(wrapper);
            }
        }
        mCustomizationAdapter.notifyDataSetChanged();
    }

    private void updateButtonWidget() {
        if (button == null) {
            return;
        }

        LinearLayout widget = button.getWidget();
        if (Game.modManager.isEmpty()) {
            Animation.of(widget)
                    .toRightPadding(0)
                    .play(100);

            widget.removeAllViews();
            return;
        }
        widget.removeAllViews();

        Animation.of(widget)
                .toRightPadding(Res.sdp(12))
                .play(100);

        for (ModWrapper wrapper : Game.modManager.getList()) {

            ModBadgeView icon = new ModBadgeView(context());
            icon.setText(wrapper.getAcronym());
            icon.setSize(Size.M);
            widget.addView(icon);

            Views.margins(icon).left(sdp(4));
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
        UI.beatmapPanel.updateAttributes();
    }

    //--------------------------------------------------------------------------------------------//

    public static class Section {

        public final String title;
        public final ArrayList<ModWrapper> mods;

        //----------------------------------------------------------------------------------------//

        public Section(String pTitle) {
            title = pTitle;
            mods = new ArrayList<>();
        }

        //----------------------------------------------------------------------------------------//

        private void add(ModWrapper pWrapper) {
            mods.add(pWrapper);
        }
    }

}
