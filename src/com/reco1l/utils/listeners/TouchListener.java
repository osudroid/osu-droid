package com.reco1l.utils.listeners;

import static com.reco1l.interfaces.IReferences.resources;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;

// Created by Reco1l on 23/6/22 20:44

public class TouchListener {

    private static final String DEFAULT_SOUND = "menuclick";

    //--------------------------------------------------------------------------------------------//

    public BassSoundProvider getClickSound() {
        return resources.getSound(DEFAULT_SOUND);
    }

    public boolean hasTouchEffect() {
        return true;
    }

    public boolean isOnlyOnce() {
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
