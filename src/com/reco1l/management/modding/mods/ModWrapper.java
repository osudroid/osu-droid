package com.reco1l.management.modding.mods;

// Created by Reco1l on 21/12/2022, 04:43

import com.reco1l.global.Game;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.annotation.Legacy;
import com.reco1l.management.modding.ModAcronyms;
import com.reco1l.ui.base.BasePreferenceFragment;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public abstract class ModWrapper implements ModAcronyms {

    public BaseViewHolder<ModWrapper> holder;

    private final Properties mProperties;

    //----------------------------------------------------------------------------------------//

    public ModWrapper(Properties properties) {
        mProperties = properties;
    }

    //----------------------------------------------------------------------------------------//

    public abstract String getName();

    public abstract String getAcronym();

    @Legacy
    public EnumSet<GameMod> getFlags() {
        return null;
    }

    //----------------------------------------------------------------------------------------//

    public final Properties getProperties() {
        return mProperties;
    }

    //----------------------------------------------------------------------------------------//

    public void onSelect(boolean isEnabled) {
        if (holder != null) {
            if (isEnabled) {
                holder.onSelect();
            } else {
                holder.onDeselect();
            }
        }
    }

    //----------------------------------------------------------------------------------------//

    public abstract static class Properties extends BasePreferenceFragment {

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            getListView().setNestedScrollingEnabled(false);
        }

        //----------------------------------------------------------------------------------------//

        public Object getProperty(String key, Object def) {
            return Game.modManager.getProperty(key, def);
        }

        public void setProperty(String key, Object value) {
            Game.modManager.setProperty(key, value);
        }
    }
}
