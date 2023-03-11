package com.reco1l.management.modding.mods;
// Created by Reco1l on 21/12/2022, 09:45

import com.reco1l.Game;
import com.reco1l.preference.SliderPreference;

import main.osu.game.mods.GameMod;
import com.rimu.R;

public class FlashlightMod extends LegacyModWrapper {

    //--------------------------------------------------------------------------------------------//

    public FlashlightMod() {
        super(new Properties(), GameMod.MOD_FLASHLIGHT);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);

        if (!isEnabled) {
            Game.modManager.setCustomFLDelay(0.12f);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class Properties extends ModWrapper.Properties {

        @Override
        protected int getPreferenceXML() {
            return R.xml.mod_custom_flashlight;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            super.onLoad();

            SliderPreference delay = find("mod_flashlight_delay");

            delay.setValueFormatter(v -> 120 * v + "ms");
            delay.setDefaultValue(1);
            delay.setMax(10);
            delay.setMin(1);

            float currentValue = Game.modManager.getCustomFLDelay();
            delay.setValue((int) (currentValue * 1000 / 120));

            delay.setOnPreferenceChangeListener((p, v) -> {
                float value = 120 * ((int) v) / 1000f;
                Game.modManager.setCustomFLDelay(value);
                return true;
            });
        }
    }
}
