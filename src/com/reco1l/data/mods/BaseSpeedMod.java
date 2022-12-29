package com.reco1l.data.mods;
// Created by Reco1l on 21/12/2022, 09:45

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;

import com.reco1l.Game;
import com.reco1l.data.LegacyModWrapper;
import com.reco1l.data.ModProperty;
import com.reco1l.data.ModWrapper;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BaseSpeedMod extends LegacyModWrapper {

    //--------------------------------------------------------------------------------------------//

    public BaseSpeedMod(GameMod mod) {
        super(mod);
    }

    @Override
    public ModWrapper.Properties getProperties() {
        return new Properties();
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
            SeekBarPreference speed = find("mod_speed");

            speed.setValue(getIntProperty(ModProperty.SpeedChange, 10));
            speed.setOnPreferenceChangeListener((p, v) -> {
                float value = 0.5f + 0.05f * speed.getValue();

                setIntProperty(ModProperty.SpeedChange, (int) v);
                Game.modMenu.setChangeSpeed(value);
                return true;
            });

            CheckBoxPreference pitch = find("mod_speed_pitch");

            pitch.setChecked(getBoolProperty(ModProperty.ShiftPitch, false));
            pitch.setOnPreferenceChangeListener((p, v) -> {
                setBoolProperty(ModProperty.ShiftPitch, (boolean) v);
                Game.modMenu.setEnableNCWhenSpeedChange((boolean) v);
                return true;
            });

            Preference reset = find("mod_speed_reset");

            reset.setOnPreferenceClickListener(p -> {
                speed.setValue(10);
                setIntProperty(ModProperty.SpeedChange, 10);
                Game.modMenu.setChangeSpeed(10);
                return true;
            });

        }
    }
}
