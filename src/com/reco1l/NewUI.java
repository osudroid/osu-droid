package com.reco1l;

import com.reco1l.ui.platform.FragmentPlatform;

// Created by Reco1l on 1/5/22 02:03

public class NewUI {

    private final FragmentPlatform platform = FragmentPlatform.getInstance();
    public EngineBridge.Scenes currentScene;
    private static NewUI instance;

    //--------------------------------------------------------------------------------------------//

    public static NewUI get() {
        if (instance == null) instance = new NewUI();
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void updateScene(EngineBridge.Scenes scene) {
        if (platform == null)
            return;
        currentScene = scene;

        //mActivity.runOnUiThread(() -> topBar().reload());

        switch(scene) {

            case LOADING_SCREEN:
                //platform.closeAllExcept(topBar);
            case MAIN_MENU:
                //platform.closeAllExcept(topBar);
            case SCORING:
                //topBar().show();
                //platform.closeAllExcept(topBar);
                break;
            case SONG_MENU:
                //topBar().show();
                //beatmapPanel().show();
                //platform.closeAllExcept(topBar, beatmapPanel);
                break;
            case GAME:
                //platform.closeAll();
                break;
        }
    }

    //-------------------------------------Global management--------------------------------------//
}
