package com.reco1l.management;

import com.reco1l.UI;
import com.reco1l.data.mods.LegacyModWrapper;
import com.reco1l.data.mods.ModWrapper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public final class ModManager {

    public final static ArrayList<ModWrapper> modList = new ArrayList<>();
    public final static EnumSet<GameMod> modSet = EnumSet.noneOf(GameMod.class);

    private final static Map<String, Object> mProperties = new HashMap<>();

    //--------------------------------------------------------------------------------------------//

    private ModManager() {}

    //--------------------------------------------------------------------------------------------//

    public static void setProperty(String key, Object value) {
        mProperties.put(key, value);
    }

    public static Object getProperty(String key, Object def) {
        if (mProperties.containsKey(key) && mProperties.get(key) != null) {
            return mProperties.get(key);
        }
        return def;
    }

    //--------------------------------------------------------------------------------------------//

    private static void handleFlags(ModWrapper mod) {
        if (mod.getFlags() == null) {
            return;
        }

        for (GameMod flag : mod.getFlags()) {
            LegacyModWrapper target = ModManager.getWrapperByGameMod(flag);

            if (target != null) {
                UI.modMenu.onModSelect(target, true);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static boolean isListEmpty() {
        return modList.isEmpty();
    }

    public static boolean contains(ModWrapper mod) {
        return modList.contains(mod);
    }

    public static boolean contains(GameMod mod) {
        return modSet.contains(mod);
    }

    public static boolean add(ModWrapper mod) {
        handleFlags(mod);

        if (mod instanceof LegacyModWrapper) {
            LegacyModWrapper legacyMod = (LegacyModWrapper) mod;
            modSet.add(legacyMod.getEntry());
        }
        return modList.add(mod);
    }

    public static boolean remove(ModWrapper mod) {
        if (mod instanceof LegacyModWrapper) {
            LegacyModWrapper legacyMod = (LegacyModWrapper) mod;

            modSet.remove(legacyMod.getEntry());
        }
        return modList.remove(mod);
    }

    public static void clear() {
        modList.clear();
        modSet.clear();
    }

    //--------------------------------------------------------------------------------------------//

    public static LegacyModWrapper getWrapperByGameMod(GameMod pMod) {
        for (ModWrapper wrapper : modList) {

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
