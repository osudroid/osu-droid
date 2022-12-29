package com.reco1l.data.mods;

// Created by Reco1l on 21/12/2022, 09:44

// Created by Reco1l on 21/12/2022, 09:42

import androidx.preference.SeekBarPreference;

import com.reco1l.Game;

import ru.nsu.ccfit.zuev.osuplus.R;

public class DifficultyAdjustMod extends ModWrapper {


    //--------------------------------------------------------------------------------------------//

    @Override
    public String getName() {
        return "Custom AR";
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public Properties getProperties() {
        return new Properties();
    }
    //--------------------------------------------------------------------------------------------//

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);
        Game.modMenu.setEnableForceAR(isEnabled);
    }


    //--------------------------------------------------------------------------------------------//

    public static class Properties extends ModWrapper.Properties {

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getPreferenceXML() {
            return R.xml.mod_custom_force_ar;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            SeekBarPreference force = find("mod_force_ar");

            force.setValue(getIntProperty(ModProperty.ForceAR, 90));
            force.setOnPreferenceChangeListener((p, v) -> {
                float value = 0.1f * ((int) v);

                setIntProperty(ModProperty.ForceAR, (int) v);
                Game.modMenu.setForceAR(value);
                return true;
            });
        }
    }

}
