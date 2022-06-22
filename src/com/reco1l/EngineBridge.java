package com.reco1l;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.menu.MenuScene;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;

// Created by Reco1l on 22/6/22 02:20

public class EngineBridge extends Engine {
    //Check which AndEngine scene is showing now and updates the new UI when setScene() is called.

    private final static GlobalManager global = GlobalManager.getInstance();
    public boolean isGlobalManagerInit = false;

    public EngineBridge(EngineOptions pEngineOptions) {
        super(pEngineOptions);
    }

    @Override
    public void setScene(Scene pScene) {
        checkScene(pScene);
        super.setScene(pScene);
    }

    private void checkScene(Scene scene) {

        if (scene instanceof LoadingScreen.LoadingScene) {
            NewUI.get().updateScene(Scenes.LOADING_SCREEN);
        }
        else if (scene instanceof MenuScene) {
            //PauseMenu is the only scene that extends MenuScene so this is a workaround for now.
            NewUI.get().updateScene(Scenes.PAUSE_MENU);
        }
        else if (isGlobalManagerInit) {
            //isGlobalManagerInit avoids a crash at first start when GlobalManager and instances are not loaded yet.
            for (Scenes toCheck: Scenes.values()) {
                if (toCheck.AndEngineScene != null && scene == toCheck.AndEngineScene) {
                    NewUI.get().updateScene(toCheck);
                    break;
                }
            }
        }
    }

    public enum Scenes {
        // LoadingScene & PauseMenu are compared with 'instanceof' because they are a different type of Scene.
        LOADING_SCREEN(null),
        PAUSE_MENU(null),

        MAIN_MENU(global.getMainScene().getScene()),
        SONG_MENU(global.getSongMenu().getScene()),
        SCORING(global.getScoring().getScene()),
        GAME(global.getGameScene().getScene());

        public Scene AndEngineScene;
        Scenes(Scene AndEngineScene) {
            this.AndEngineScene = AndEngineScene;
        }
    }
}
