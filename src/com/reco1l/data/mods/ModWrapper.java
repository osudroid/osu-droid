package com.reco1l.data.mods;

// Created by Reco1l on 21/12/2022, 04:43

import com.reco1l.UI;
import com.reco1l.ui.BasePreferenceFragment;
import com.reco1l.data.adapters.ModListAdapter.ModViewHolder;

public abstract class ModWrapper {

    public ModViewHolder holder;

    private Properties properties;

    //----------------------------------------------------------------------------------------//

    public ModWrapper() {
        properties = createProperties();
    }

    //----------------------------------------------------------------------------------------//

    public abstract String getName();

    public abstract String getIcon();

    protected abstract Properties createProperties();

    public final Properties getProperties() {
        return properties;
    }

    //----------------------------------------------------------------------------------------//

    public void onSelect(boolean isEnabled) {
        if (holder != null) {
            holder.onModSelect(isEnabled);
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
            return UI.modMenu.getProperty(key, def);
        }

        public void setProperty(String key, Object value) {
            UI.modMenu.setProperty(key, value);
        }
    }
}
