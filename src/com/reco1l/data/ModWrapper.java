package com.reco1l.data;

// Created by Reco1l on 21/12/2022, 04:43

import com.reco1l.UI;
import com.reco1l.ui.custom.BasePreferenceFragment;
import com.reco1l.ui.data.ModListAdapter.ModViewHolder;

public abstract class ModWrapper {

    public ModViewHolder holder;

    //----------------------------------------------------------------------------------------//

    public abstract String getName();

    public abstract String getIcon();

    public abstract Properties getProperties();

    //----------------------------------------------------------------------------------------//

    public void onSelect(boolean isEnabled) {
        if (holder != null) {
            holder.onSelect(isEnabled);
        }
    }

    //----------------------------------------------------------------------------------------//

    public abstract static class Properties extends BasePreferenceFragment {

        public Integer getIntProperty(String key, int def) {
            return UI.modMenu.getIntProperty(key, def);
        }

        public Float getFloatProperty(String key, float def) {
            return UI.modMenu.getFloatProperty(key, def);
        }

        public Boolean getBoolProperty(String key, boolean def) {
            return UI.modMenu.getBoolProperty(key, def);
        }

        //----------------------------------------------------------------------------------------//

        public void setIntProperty(String key, int value) {
            UI.modMenu.setIntProperty(key, value);
        }

        public void setFloatProperty(String key, float value) {
            UI.modMenu.setFloatProperty(key, value);
        }

        public void setBoolProperty(String key, boolean value) {
            UI.modMenu.setBoolProperty(key, value);
        }
    }
}
