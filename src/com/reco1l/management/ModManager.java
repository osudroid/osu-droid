package com.reco1l.management;

import com.reco1l.UI;
import com.reco1l.data.mods.LegacyModWrapper;
import com.reco1l.data.mods.ModWrapper;
import com.reco1l.legacy.Legacy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public final class ModManager {

    public static final ModManager instance = new ModManager();

    private final Map<String, Object> mProperties;
    private final ArrayList<ModWrapper> mModList;

    @Legacy
    private final EnumSet<GameMod> mModSet;

    //--------------------------------------------------------------------------------------------//

    public ModManager() {
        mModList = new ArrayList<>();
        mProperties = new HashMap<>();
        mModSet = EnumSet.noneOf(GameMod.class);
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public ArrayList<ModWrapper> getList() {
        return mModList;
    }

    public EnumSet<GameMod> getSet() {
        return mModSet;
    }

    //--------------------------------------------------------------------------------------------//

    public void setProperty(String key, Object value) {
        mProperties.put(key, value);
    }

    public Object getProperty(String key, Object def) {
        if (mProperties.containsKey(key) && mProperties.get(key) != null) {
            return mProperties.get(key);
        }
        return def;
    }

    //--------------------------------------------------------------------------------------------//

    private void handleFlags(ModWrapper mod) {
        if (mod.getFlags() == null) {
            return;
        }

        for (GameMod flag : mod.getFlags()) {
            LegacyModWrapper target = getWrapperByGameMod(flag);

            if (target != null) {
                UI.modMenu.onModSelect(target, true);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isEmpty() {
        return mModList.isEmpty();
    }

    public boolean contains(ModWrapper mod) {
        return mModList.contains(mod);
    }

    public boolean contains(GameMod mod) {
        return mModSet.contains(mod);
    }

    public boolean add(ModWrapper mod) {
        handleFlags(mod);

        if (mod instanceof LegacyModWrapper) {
            LegacyModWrapper legacyMod = (LegacyModWrapper) mod;
            mModSet.add(legacyMod.getEntry());
        }
        return mModList.add(mod);
    }

    public boolean remove(ModWrapper mod) {
        if (mod instanceof LegacyModWrapper) {
            LegacyModWrapper legacyMod = (LegacyModWrapper) mod;

            mModSet.remove(legacyMod.getEntry());
        }
        return mModList.remove(mod);
    }

    public void clear() {
        mModList.clear();
        mModSet.clear();
    }

    //--------------------------------------------------------------------------------------------//

    public LegacyModWrapper getWrapperByGameMod(GameMod pMod) {
        for (ModWrapper wrapper : mModList) {

            if (wrapper instanceof LegacyModWrapper) {
                LegacyModWrapper legacyWrapper = (LegacyModWrapper) wrapper;

                if (legacyWrapper.getEntry() == pMod) {
                    return legacyWrapper;
                }
            }
        }
        return null;
    }
}
