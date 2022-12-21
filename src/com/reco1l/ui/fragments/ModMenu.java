package com.reco1l.ui.fragments;

// Created by Reco1l on 20/12/2022, 05:40

import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.Game;
import com.reco1l.interfaces.IGameMods;
import com.reco1l.ui.data.ModListAdapter;
import com.reco1l.ui.data.ModSectionAdapter;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Res;
import com.reco1l.view.BarButton;
import com.reco1l.view.TextButton;

import java.util.ArrayList;
import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ModMenu extends UIFragment implements IGameMods {

    public static ModMenu instance;

    public BarButton button;

    public EnumSet<GameMod> mods;
    public ArrayList<ModWrapper> enabled;
    public ArrayList<Section> sections;

    public Section
            increase,
            reduction,
            automation,
            miscellaneous;

    private ModSectionAdapter adapter;

    //--------------------------------------------------------------------------------------------//

    public ModMenu() {
        super();
        enabled = new ArrayList<>();
        sections = new ArrayList<>();
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

    //--------------------------------------------------------------------------------------------//

    private void loadMods() {
        sections.add(increase = new Section("Difficulty Increase"));
        sections.add(reduction = new Section("Difficulty Reduction"));
        sections.add(automation = new Section("Automation"));
        sections.add(miscellaneous = new Section("Miscellaneous"));

        for (int i = 0; i < GameMod.values().length; ++i) {
            GameMod mod = GameMod.values()[i];
            Section section = miscellaneous;

            if (mod.equals(EZ, NF, HT, REZ)) {
                section = reduction;
            }
            if (mod.equals(HR, DT, NC, HD, PR, SD, SC, PF, FL)) {
                section = increase;
            }
            if (mod.equals(RX, AU, AP)) {
                section = automation;
            }
            section.modWrappers.add(new ModWrapper(mod));
        }

        adapter = new ModSectionAdapter(sections);
    }

    @Override
    protected void onLoad() {
        button.setActivated(true);

        if (adapter == null) {
            loadMods();
        }

        RecyclerView recyclerView = find("sectionContainer");
        recyclerView.setAdapter(adapter);

        TextButton clear = find("clear");
        bindTouchListener(clear, this::clear);
    }

    //--------------------------------------------------------------------------------------------//

    public void clear() {
        for (ModWrapper mod : enabled) {
            mod.holder.setEnabledVisually(false);
        }
        enabled.clear();
        mods.clear();
        updateButtonWidget();
    }

    //--------------------------------------------------------------------------------------------//

    public void onModSelect(ModWrapper mod) {
        if (enabled.contains(mod)) {
            mod.holder.setEnabledVisually(false);
            enabled.remove(mod);

            if (mod.gameMod != null) {
                mods.remove(mod.gameMod);
            }
        } else {
            handleFlags(mod);
            mod.holder.setEnabledVisually(true);
            enabled.add(mod);
            if (mod.gameMod != null) {
                mods.add(mod.gameMod);
            }
        }
        updateButtonWidget();
    }

    private void handleFlags(ModWrapper modWrapper) {
        if (modWrapper.flags == null) {
            // This means the mod is compatible with every one
            return;
        }

        for (GameMod mod : modWrapper.flags) {
            ModWrapper target = getWrapperByGameMod(mod);

            if (target != null) {
                target.holder.setEnabledVisually(false);
                enabled.remove(target);
                if (target.gameMod != null) {
                    mods.remove(target.gameMod);
                }
            }
        }
    }

    private ModWrapper getWrapperByGameMod(GameMod mod) {
        for (ModWrapper wrapper : enabled) {
            if (wrapper.gameMod == mod) {
                return wrapper;
            }
        }
        return null;
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
            if (wrapper.gameMod != null) {

                ImageView icon = new ImageView(getContext());
                icon.setImageBitmap(Game.bitmapManager.get("selection-mod-" + wrapper.gameMod.texture));

                widget.addView(icon);

                Animation.of(icon)
                        .fromWidth(0)
                        .toWidth(Res.sdp(16))
                        .toLeftMargin(Res.sdp(4))
                        .play(100);
            }
        }
    }

    @Override
    public void close() {
        super.close();
        button.setActivated(false);
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
    }

    //--------------------------------------------------------------------------------------------//

    public static class ModWrapper {

        public GameMod gameMod;
        public EnumSet<GameMod> flags;
        public ModListAdapter.ModHolder holder;

        //----------------------------------------------------------------------------------------//

        public ModWrapper(GameMod gameMod) {
            this.gameMod = gameMod;
            setFlags(gameMod);
        }

        public ModWrapper() {
        }

        //----------------------------------------------------------------------------------------//

        private void setFlags(GameMod mod) {
            switch (mod) {
                case MOD_HARDROCK:
                case MOD_EASY:
                    flags = EnumSet.of(EZ, HR);
                    break;

                case MOD_DOUBLETIME:
                case MOD_NIGHTCORE:
                case MOD_HALFTIME:
                    flags = EnumSet.of(NC, HT, DT);
                    break;

                case MOD_AUTO:
                case MOD_RELAX:
                case MOD_NOFAIL:
                case MOD_PERFECT:
                case MOD_AUTOPILOT:
                case MOD_SUDDENDEATH:
                    flags = EnumSet.of(AU, RX, NF, PF, AP, SD);
                    break;
            }

            if (flags != null) {
                flags.remove(mod);
            }
        }

    }
}
