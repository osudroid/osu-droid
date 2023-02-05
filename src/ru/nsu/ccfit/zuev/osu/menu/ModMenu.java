package ru.nsu.ccfit.zuev.osu.menu;

import static ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*;

import com.reco1l.global.Game;

import org.anddev.andengine.entity.text.ChangeableText;

import java.util.EnumSet;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ModMenu {
    private static final ModMenu instance = new ModMenu();
    private EnumSet<GameMod> mod;
    private ChangeableText multiplierText;
    private float changeSpeed = 1.0f;
    private float forceAR = 9.0f;
    private boolean enableForceAR = false;
    private boolean enableNCWhenSpeedChange = false;
    private float FLfollowDelay = 0.12f;

    private ModMenu() {
        mod = EnumSet.noneOf(GameMod.class);
    }

    public float getFLfollowDelay() {
        return FLfollowDelay;
    }

    public void setFLfollowDelay(float newfLfollowDelay) {
        FLfollowDelay = newfLfollowDelay;
    }
    
    public static ModMenu getInstance() {
        return instance;
    }

    public EnumSet<GameMod> getMod() {
        return Game.modManager.getSet();
    }

    public void setMod(EnumSet<GameMod> mod) {
        this.mod = mod.clone();
    }

    private void changeMultiplierText() {
        float mult = 1;
        for (GameMod m : mod) {
            mult *= m.scoreMultiplier;
        }
        if (changeSpeed != 1.0f){
            mult *= StatisticV2.getSpeedChangeScoreMultiplier(getSpeed(), mod);
        }

        multiplierText.setText(StringTable.format(R.string.menu_mod_multiplier,
                mult));
        multiplierText.setPosition(
                Config.getRES_WIDTH() / 2f - multiplierText.getWidth() / 2,
                multiplierText.getY());
        if (mult == 1) {
            multiplierText.setColor(1, 1, 1);
        } else if (mult < 1) {
            multiplierText.setColor(1, 150f / 255f, 0);
        } else {
            multiplierText.setColor(5 / 255f, 240 / 255f, 5 / 255f);
        }
    }

    public float getSpeed(){
        float speed = changeSpeed;
        if (mod.contains(MOD_DOUBLETIME) || mod.contains(MOD_NIGHTCORE)){
            speed *= 1.5f;
        } else if (mod.contains(MOD_HALFTIME)) {
            speed *= 0.75f;
        }

        return speed;
    }

    public boolean isChangeSpeed() {
        return changeSpeed != 1.0;
    }

    public float getChangeSpeed(){
        return changeSpeed;
    }

    public void setChangeSpeed(float speed){
        changeSpeed = speed;
    }

    public float getForceAR(){
        return forceAR;
    }

    public void setForceAR(float ar){
        forceAR = ar;
    }

    public boolean isEnableForceAR(){
        return enableForceAR;
    }

    public void setEnableForceAR(boolean t){
        enableForceAR = t;
    }

    public boolean isEnableNCWhenSpeedChange(){
        return enableNCWhenSpeedChange;
    }

    public void setEnableNCWhenSpeedChange(boolean t){
        enableNCWhenSpeedChange = t;
    }

    public void updateMultiplierText(){
        changeMultiplierText();
    }

}
