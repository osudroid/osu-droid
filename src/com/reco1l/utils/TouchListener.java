package com.reco1l.utils;

import com.reco1l.Game;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;

// Created by Reco1l on 23/6/22 20:44

public class TouchListener {

    //--------------------------------------------------------------------------------------------//

    public BassSoundProvider getPressDownSound() {
        return null;
    }

    public BassSoundProvider getPressUpSound() {
        return Game.resourcesManager.getSound("menuclick");
    }

    public BassSoundProvider getLongPressSound() {
        return null;
    }

    public boolean useOnlyOnce() {
        return false;
    }

    public boolean useTouchEffect() {
        return true;
    }

    public boolean useBorderlessEffect() {
        return false;
    }

    //--------------------------------------------------------------------------------------------//


    public void onPressDown() {
        // Do something
    }

    public void onPressUp() {
        // Do something
    }

    public void onLongPress() {
        // Do something
    }
}
