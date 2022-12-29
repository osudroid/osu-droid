package com.reco1l.data.mods;
// Created by Reco1l on 21/12/2022, 09:45

import androidx.preference.SeekBarPreference;

import com.reco1l.Game;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FlashlightMod extends LegacyModWrapper {

    //--------------------------------------------------------------------------------------------//

    public FlashlightMod() {
        super(GameMod.MOD_FLASHLIGHT);
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
            return R.xml.mod_custom_flashlight;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            SeekBarPreference delay = find("mod_fl_delay");

            delay.setValue(getIntProperty(ModProperty.FlDelay, 1));
            delay.setOnPreferenceChangeListener((p, v) -> {
                float value = Math.round(((int) v) * 1200f) / (10f * 1000f);

                setIntProperty(ModProperty.FlDelay, (int) v);
                Game.modMenu.setFLfollowDelay(value);
                return true;
            });
        }
    }
}
