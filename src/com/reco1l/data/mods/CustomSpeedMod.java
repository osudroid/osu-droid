package com.reco1l.data.mods;
// Created by Reco1l on 21/12/2022, 09:45

import androidx.preference.CheckBoxPreference;

import com.reco1l.Game;

import com.reco1l.preference.GameCheckBoxPreference;
import com.reco1l.preference.GameSekBarPreference;
import ru.nsu.ccfit.zuev.osuplus.R;

public class CustomSpeedMod extends ModWrapper {

    //--------------------------------------------------------------------------------------------//

    public CustomSpeedMod() {
        super();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public String getName() {
        return "Custom Speed";
    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public ModWrapper.Properties createProperties() {
        return new Properties();
    }

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Properties extends ModWrapper.Properties {

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getPreferenceXML() {
            return R.xml.mod_custom_speed;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            super.onLoad();

            GameSekBarPreference speed = find("mod_speed");

            speed.setDefaultValue(100);
            speed.setMax(200);
            speed.setMin(50);

            speed.setValue((int) ((float) getProperty(ModProperty.CustomSpeed_Value, 1.0f) * 100));
            speed.setValueFormatter(value -> (float) value / 100 + "x");

            speed.setOnPreferenceChangeListener((p, v) -> {
                setProperty(ModProperty.CustomSpeed_Value, ((int) v) / (float) 100);

                Game.modMenu.setChangeSpeed(((int) v) / (float) 100);
                return true;
            });

            GameCheckBoxPreference pitch = find("mod_speed_pitch");

            pitch.setDefaultValue(false);
            pitch.setChecked((boolean) getProperty(ModProperty.CustomSpeed_ShiftPitch, false));

            pitch.setOnPreferenceChangeListener((p, v) -> {
                setProperty(ModProperty.CustomSpeed_ShiftPitch, v);
                Game.modMenu.setEnableNCWhenSpeedChange((boolean) v);
                return true;
            });
        }
    }
}
