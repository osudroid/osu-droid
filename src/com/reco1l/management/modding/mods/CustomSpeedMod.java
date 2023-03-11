package com.reco1l.management.modding.mods;
// Created by Reco1l on 21/12/2022, 09:45

import com.reco1l.Game;
import com.reco1l.preference.CheckPreference;
import com.reco1l.preference.SliderPreference;

import java.util.EnumSet;

import main.osu.game.mods.GameMod;
import com.rimu.R;

public class CustomSpeedMod extends ModWrapper {

    //--------------------------------------------------------------------------------------------//

    public CustomSpeedMod() {
        super(new Properties());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public String getName() {
        return "Custom Speed";
    }

    @Override
    public String getAcronym() {
        return "cs";
    }

    @Override
    public EnumSet<GameMod> getFlags() {
        return EnumSet.of(DT, NC, HT);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onSelect(boolean isEnabled) {
        super.onSelect(isEnabled);

        if (!isEnabled) {
            Game.modManager.setCustomSpeed(1);
            Game.modManager.setPitchShift(false);
            updatePlayback();
        }
    }

    private static void updatePlayback() {
        Game.musicManager.setPlayback(
                Game.modManager.getCustomSpeed(),
                Game.modManager.isPitchShift()
        );
    }

    //--------------------------------------------------------------------------------------------//

    public static class Properties extends ModWrapper.Properties {

        @Override
        protected int getPreferenceXML() {
            return R.xml.mod_custom_speed;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            super.onLoad();

            CheckPreference pitch = find("mod_speed_pitch");

            pitch.setDefaultValue(false);
            pitch.setChecked(Game.modManager.isPitchShift());

            pitch.setOnPreferenceChangeListener((p, v) -> {
                Game.modManager.setPitchShift((boolean) v);
                updatePlayback();
                return true;
            });

            SliderPreference speed = find("mod_speed");

            speed.setDefaultValue(100);
            speed.setMax(200);
            speed.setMin(50);

            speed.setValue((int) Game.modManager.getCustomSpeed() * 100);
            speed.setValueFormatter(value -> (float) value / 100 + "x");

            speed.setOnPreferenceChangeListener((p, v) -> {
                Game.modManager.setCustomSpeed(((int) v) / (float) 100);
                updatePlayback();
                return true;
            });
        }
    }
}
