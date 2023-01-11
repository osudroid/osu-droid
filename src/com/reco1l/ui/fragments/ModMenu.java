package com.reco1l.ui.fragments;

// Created by Reco1l on 20/12/2022, 05:40

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.factor.bouncy.BouncyRecyclerView;
import com.reco1l.Game;
import com.reco1l.data.ModCustomizationAdapter;
import com.reco1l.data.mods.*;
import com.reco1l.interfaces.IGameMods;
import com.reco1l.data.adapters.ModSectionAdapter;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.tables.Res;
import com.reco1l.view.IconButton;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

public final class ModMenu extends BaseFragment implements IGameMods {

    public static ModMenu instance;

    public IconButton button;

    public Section
            increase,
            reduction,
            automation,
            miscellaneous;

    public final ArrayList<ModWrapper> enabled;
    public final EnumSet<GameMod> mods;

    private final Map<String, Object> properties;

    private ModCustomizationAdapter customAdapter;
    private BouncyRecyclerView customization;
    private ModSectionAdapter adapter;

    //--------------------------------------------------------------------------------------------//

    public ModMenu() {
        super();
        enabled = new ArrayList<>();
        properties = new HashMap<>();
        mods = EnumSet.noneOf(GameMod.class);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.mod_menu;
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
                increase = new Section("Difficulty Increase"),
                reduction = new Section("Difficulty Reduction"),
                automation = new Section("Automation"),
                miscellaneous = new Section("Miscellaneous")
        };

        for (int i = 0; i < GameMod.values().length; ++i) {
            GameMod mod = GameMod.values()[i];
            Section section = miscellaneous;

            if (mod.equals(EZ, NF, HT, REZ)) {
                section = reduction;
            }
            if (mod.equals(HR, DT, NC, HD, PR, SD, SC, PF, FL)) {
                section = increase;

                if (mod.equals(FL)) {
                    section.add(new FlashlightMod());
                    continue;
                }
            }
            if (mod.equals(RX, AU, AP)) {
                section = automation;
            }
            section.add(new LegacyModWrapper(mod));
        }
        loadCustomMods();

        adapter = new ModSectionAdapter(sections);
        customAdapter = new ModCustomizationAdapter(new ArrayList<>());
    }

    private void loadCustomMods() {
        miscellaneous.add(new CustomDifficultyMod());
        miscellaneous.add(new CustomSpeedMod());
    }

    @Override
    protected void onLoad() {
        button.setActivated(true);

        if (adapter == null) {
            loadMods();
        }

        RecyclerView recyclerView = find("sectionContainer");
        recyclerView.setAdapter(adapter);

        customization = find("customRv");
        customization.setLayoutManager(new LinearLayoutManager(getContext(), HORIZONTAL, false));
        customization.setAdapter(customAdapter);

        bindTouch(find("clear"), this::clear);
        bindTouch(find("custom"), this::switchCustomVisibility);
    }

    //--------------------------------------------------------------------------------------------//

    private void switchCustomVisibility() {
        if (customization.getVisibility() != View.VISIBLE) {
            customization.setVisibility(View.VISIBLE);
        } else {
            customization.setVisibility(View.GONE);
        }
    }

    public void clear() {
        for (ModWrapper mod : enabled) {
            mod.holder.onModSelect(false);
        }
        enabled.clear();
        mods.clear();

        updateCustomizations();
        updateButtonWidget();
    }

    public void onModSelect(ModWrapper wrapper, boolean byFlag) {
        if (enabled.remove(wrapper)) {
            wrapper.onSelect(false);
        }
        else if (enabled.add(wrapper) && !byFlag) {
            wrapper.onSelect(true);
        }
        updateCustomizations();
        updateButtonWidget();
    }

    public LegacyModWrapper getWrapperByGameMod(GameMod mod) {
        for (ModWrapper wrapper : enabled) {

            if (wrapper instanceof LegacyModWrapper) {
                LegacyModWrapper legacyWrapper = (LegacyModWrapper) wrapper;

                if (legacyWrapper.mod == mod) {
                    return legacyWrapper;
                }
            }
        }
        return null;
    }

    private void updateCustomizations() {
        customAdapter.clear();

        for (ModWrapper wrapper : enabled) {
            if (wrapper.getProperties() != null) {
                customAdapter.getData().add(wrapper);
            }
        }
        customAdapter.notifyDataSetChanged();
    }

    private void updateButtonWidget() {
        if (button == null) {
            return;
        }

        LinearLayout widget = button.getWidget();
        if (enabled.isEmpty()) {
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

        for (ModWrapper wrapper : enabled) {
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

    @Override
    public void close() {
        super.close();
        button.setActivated(false);
    }


    //--------------------------------------------------------------------------------------------//

    public Object getProperty(String key, Object def) {
        if (properties.containsKey(key)) {
            return properties.get(key);
        }
        return def;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Section {

        public final String title;
        public final ArrayList<ModWrapper> modWrappers;

        //----------------------------------------------------------------------------------------//

        public Section(String title) {
            this.title = title;
            this.modWrappers = new ArrayList<>();
        }

        //----------------------------------------------------------------------------------------//

        private void add(ModWrapper wrapper) {
            modWrappers.add(wrapper);
        }
    }

}
