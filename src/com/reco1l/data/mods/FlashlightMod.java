package com.reco1l.data.mods;
// Created by Reco1l on 21/12/2022, 09:45

import com.reco1l.Game;
import com.reco1l.management.ModProperty;
import com.reco1l.preference.GameSekBarPreference;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FlashlightMod extends LegacyModWrapper {

    //--------------------------------------------------------------------------------------------//

    public FlashlightMod() {
        super(GameMod.MOD_FLASHLIGHT);
    }

    @Override
    public ModWrapper.Properties createProperties() {
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
            super.onLoad();

            GameSekBarPreference delay = find("mod_flashlight_delay");

            delay.setValueFormatter(v -> 120 * v + "ms");
            delay.setDefaultValue(1);
            delay.setMax(10);
            delay.setMin(1);

            float currentValue = (float) getProperty(ModProperty.Flashlight_Delay, 0.12f);
            delay.setValue((int) (currentValue * 1000 / 120));

            delay.setOnPreferenceChangeListener((p, v) -> {
                float value = 120 * ((int) v) / 1000f;

                setProperty(ModProperty.Flashlight_Delay, value);
                Game.modMenu.setFLfollowDelay(value);
                return true;
            });
        }
    }
}
