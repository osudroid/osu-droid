package com.reco1l.data.mods;

// Created by Reco1l on 21/12/2022, 04:42

import com.reco1l.UI;
import com.reco1l.interfaces.IGameMods;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class LegacyModWrapper extends ModWrapper implements IGameMods {

    public GameMod mod;
    public EnumSet<GameMod> flags;

    //----------------------------------------------------------------------------------------//

    public LegacyModWrapper(GameMod mod) {
        this.mod = mod;
        parseFlags(mod);
    }

    //----------------------------------------------------------------------------------------//

    @Override
    public String getName() {
        return mod.name().toLowerCase().replace("mod_", "");
    }

    @Override
    public String getIcon() {
        return "selection-mod-" + mod.texture;
    }

    @Override
    public Properties getProperties() {
        switch (mod) {
            case MOD_DOUBLETIME:
            case MOD_NIGHTCORE:
            case MOD_HALFTIME:
                return new BaseSpeedMod.Properties();
        }
        return null;
    }
    //----------------------------------------------------------------------------------------//

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);
        if (isEnabled) {
            handleFlags();
            UI.modMenu.mods.add(mod);
        } else {
            UI.modMenu.mods.remove(mod);
        }
    }

    //----------------------------------------------------------------------------------------//

    private void parseFlags(GameMod mod) {
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

    private void handleFlags() {
        if (flags == null) {
            return;
        }

        for (GameMod mod : flags) {
            LegacyModWrapper target = UI.modMenu.getWrapperByGameMod(mod);

            if (target != null) {
                UI.modMenu.onModSelect(target, true);
            }
        }
    }

}
