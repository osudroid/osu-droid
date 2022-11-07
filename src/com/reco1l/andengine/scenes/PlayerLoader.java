package com.reco1l.andengine.scenes;

import com.reco1l.andengine.OsuScene;
import com.reco1l.enums.Screens;

public class PlayerLoader extends OsuScene {

    public static PlayerLoader instance;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.PLAYER_LOADER;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setTimingWrapper(false);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {

    }

    //--------------------------------------------------------------------------------------------//


}
