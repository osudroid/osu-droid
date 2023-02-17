package com.reco1l.scenes;

import com.reco1l.global.Game;

import org.anddev.andengine.entity.scene.Scene;

// TODO [ListingScene] Replace WebView with UI and Chimu API usage
public class ListingScene extends BaseScene {

    public static final ListingScene instance = new ListingScene();

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onSceneChange(Scene lastScene, Scene newScene) {
        super.onSceneChange(lastScene, newScene);

        if (newScene == this) {
            Game.musicManager.pause();
        } else if (lastScene == this) {
            Game.musicManager.play();
        }
    }
}
