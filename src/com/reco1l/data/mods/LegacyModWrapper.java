package com.reco1l.data.mods;

// Created by Reco1l on 21/12/2022, 04:42

import com.reco1l.annotation.Legacy;
import com.reco1l.global.Game;
import com.reco1l.interfaces.IGameMod;
import com.reco1l.utils.helpers.ScoringHelper;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

@Legacy // Handles mod entries from GameMod class
public class LegacyModWrapper extends ModWrapper implements IGameMod {

    private final GameMod mMod;

    private EnumSet<GameMod> mFlags;

    //----------------------------------------------------------------------------------------//

    public LegacyModWrapper(Properties properties, GameMod entry) {
        super(properties);
        mMod = entry;
        parseFlags();
    }

    public LegacyModWrapper(GameMod entry) {
        this(null, entry);
    }

    //----------------------------------------------------------------------------------------//

    @Override
    public String getName() {
        return mMod.name().toLowerCase().replace("mod_", "");
    }

    public GameMod getEntry() {
        return mMod;
    }

    @Override
    public String getAcronym() {
        return ScoringHelper.parseAcronym(mMod);
    }

    @Override
    public EnumSet<GameMod> getFlags() {
        return mFlags;
    }

    //----------------------------------------------------------------------------------------//

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);

        if (isEnabled) {
            switch (mMod) {
                case MOD_DOUBLETIME:
                    Game.musicManager.setPlayback(1.5f, false);
                    break;
                case MOD_NIGHTCORE:
                    Game.musicManager.setPlayback(1.5f, true);
                    break;
                case MOD_HALFTIME:
                    Game.musicManager.setPlayback(0.75f, false);
                    break;
            }
        } else {
            switch (mMod) {
                case MOD_DOUBLETIME:
                case MOD_NIGHTCORE:
                case MOD_HALFTIME:
                    Game.musicManager.setPlayback(1f, false);
                    break;
            }
        }
    }

    //----------------------------------------------------------------------------------------//

    private void parseFlags() {

        switch (mMod) {
            case MOD_HARDROCK:
            case MOD_EASY:
                mFlags = EnumSet.of(EZ, HR);
                break;

            case MOD_DOUBLETIME:
            case MOD_NIGHTCORE:
            case MOD_HALFTIME:
                mFlags = EnumSet.of(NC, HT, DT);
                break;

            case MOD_AUTO:
            case MOD_RELAX:
            case MOD_NOFAIL:
            case MOD_PERFECT:
            case MOD_AUTOPILOT:
            case MOD_SUDDENDEATH:
                mFlags = EnumSet.of(AU, RX, NF, PF, AP, SD);
                break;
        }

        if (mFlags != null) {
            mFlags.remove(mMod);
        }
    }
}
