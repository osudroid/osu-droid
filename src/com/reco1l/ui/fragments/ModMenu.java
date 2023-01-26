package com.reco1l.ui.fragments;

// Created by Reco1l on 20/12/2022, 05:40

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.factor.bouncy.BouncyRecyclerView;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.adapters.ModCustomizationAdapter;
import com.reco1l.interfaces.IGameMod;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.view.IconButton;

import com.reco1l.data.mods.CustomDifficultyMod;
import com.reco1l.data.mods.CustomSpeedMod;
import com.reco1l.data.mods.FlashlightMod;
import com.reco1l.data.mods.LegacyModWrapper;
import com.reco1l.data.mods.ModWrapper;
import com.reco1l.data.adapters.ModSectionAdapter;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

public final class ModMenu extends BaseFragment implements IGameMod {

    public static ModMenu instance;

    public IconButton button;

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
        return R.layout.extra_mod_menu;
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
        button.setActivated(true);

        if (mSectionAdapter == null) {
            loadMods();
        }

        RecyclerView recyclerView = find("sectionContainer");
        recyclerView.setAdapter(mSectionAdapter);

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
            if (wrapper.getIcon() == null) {
                continue;
            }

            ImageView icon = new ImageView(getContext());
            icon.setImageBitmap(Game.bitmapManager.get(wrapper.getIcon()));

            widget.addView(icon);

            Animation.of(icon)
                    .fromWidth(0)
                    .toWidth(Res.sdp(16))
                    .toLeftMargin(Res.sdp(4))
                    .play(100);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        super.close();
        UI.beatmapPanel.updateAttributes();
        button.setActivated(false);
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
