package com.reco1l.management.modding;

import static main.osu.game.mods.GameMod.MOD_DOUBLETIME;
import static main.osu.game.mods.GameMod.MOD_HALFTIME;
import static main.osu.game.mods.GameMod.MOD_NIGHTCORE;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.management.modding.mods.LegacyModWrapper;
import com.reco1l.management.modding.mods.ModWrapper;
import com.reco1l.annotation.Legacy;

import java.util.ArrayList;
import java.util.EnumSet;

import main.osu.game.mods.GameMod;
import main.osu.scoring.Replay;
import main.osu.scoring.StatisticV2;

public final class ModManager {

    public static final ModManager instance = new ModManager();

    private final ArrayList<ModWrapper> mModList;

    // Since old code is shit i'll remain this things

    @Legacy
    private EnumSet<GameMod> mModSet;

    @Legacy
    private float
            mCustomAR = -1,
            mCustomSpeed = 1f,
            mCustomFLDelay = 0.12f;

    @Legacy
    private boolean mPitchShift;

    //--------------------------------------------------------------------------------------------//

    public ModManager() {
        mModList = new ArrayList<>();
        mModSet = EnumSet.noneOf(GameMod.class);
    }

    //--------------------------------------------------------------------------------------------//

    public ArrayList<ModWrapper> getList() {
        return mModList;
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

    public float getScoreMultiplier() {
        float value = 1;

        for (GameMod m : mModSet) {
            value *= m.scoreMultiplier;
        }
        if (isCustomSpeed()) {
            value *= StatisticV2.getSpeedChangeScoreMultiplier(getSpeed(), mModSet);
        }

        return value;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public EnumSet<GameMod> getMods() {
        return mModSet;
    }

    @Legacy
    public void setMods(EnumSet<GameMod> mods) {
        mModSet = EnumSet.noneOf(GameMod.class);

        if (mods != null) {
            for (GameMod mod : mods) {
                add(getWrapperByGameMod(mod));
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public float getCustomFLDelay() {
        return mCustomFLDelay;
    }

    @Legacy
    public void setCustomFLDelay(float delay) {
        mCustomFLDelay = delay;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public float getSpeed() {
        float speed = mCustomSpeed;

        if (mModSet.contains(MOD_DOUBLETIME) || mModSet.contains(MOD_NIGHTCORE)) {
            speed *= 1.5f;
        } else if (mModSet.contains(MOD_HALFTIME)) {
            speed *= 0.75f;
        }

        return speed;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public boolean isCustomSpeed() {
        return mCustomSpeed != 1.0;
    }

    @Legacy
    public float getCustomSpeed() {
        return mCustomSpeed;
    }

    @Legacy
    public void setCustomSpeed(float speed) {
        mCustomSpeed = speed;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public float getCustomAR() {
        return mCustomAR;
    }

    @Legacy
    public void setCustomAR(float ar) {
        mCustomAR = ar;
    }

    @Legacy
    public boolean isCustomAR() {
        return mCustomAR != -1;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public boolean isPitchShift() {
        return mPitchShift;
    }

    @Legacy
    public void setPitchShift(boolean shift) {
        mPitchShift = shift;
    }

    //--------------------------------------------------------------------------------------------//

    @Legacy
    public void saveValues() {
        Replay.oldMod = getMods();
        Replay.oldChangeSpeed = getCustomSpeed();
        Replay.oldForceAR = getCustomAR();
        Replay.oldEnableForceAR = isCustomAR();
        Replay.oldFLFollowDelay = getCustomFLDelay();
    }

    @Legacy
    public void resetValues() {
        setMods(Replay.oldMod);
        setCustomSpeed(Replay.oldChangeSpeed);
        if (Replay.oldEnableForceAR) {
            setCustomAR(Replay.oldForceAR);
        } else {
            setCustomAR(-1);
        }
        setCustomFLDelay(Replay.oldFLFollowDelay);
    }

    @Legacy
    public void setFromStats(StatisticV2 stats) {
        setMods(stats.getMod());
        setCustomSpeed(stats.getChangeSpeed());

        if (stats.isEnableForceAR()) {
            setCustomAR(stats.getForceAR());
        } else {
            setCustomAR(-1);
        }
        setCustomFLDelay(stats.getFLFollowDelay());
    }
}
