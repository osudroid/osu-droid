package com.reco1l.data.mods;

// Created by Reco1l on 21/12/2022, 04:43

import com.reco1l.data.BaseViewHolder;
import com.reco1l.interfaces.IGameMod;
import com.reco1l.legacy.Legacy;
import com.reco1l.management.ModManager;
import com.reco1l.ui.BasePreferenceFragment;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.audio.serviceAudio.PlayMode;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public abstract class ModWrapper implements IGameMod {

    public BaseViewHolder<ModWrapper> holder;

    private final Properties mProperties;

    //----------------------------------------------------------------------------------------//

    public ModWrapper() {
        mProperties = createProperties();
    }

    //----------------------------------------------------------------------------------------//

    public abstract String getName();

    public abstract String getIcon();

    protected abstract Properties createProperties();

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
            return ModManager.getProperty(key, def);
        }

        public void setProperty(String key, Object value) {
            ModManager.setProperty(key, value);
        }
    }
}
