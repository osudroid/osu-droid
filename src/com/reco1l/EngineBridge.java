package com.reco1l;

import com.reco1l.ui.TopBar;
import com.reco1l.utils.ILayouts;
import com.reco1l.utils.IMainClasses;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.menu.MenuScene;

import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;

// Created by Reco1l on 22/6/22 02:20

public class EngineBridge extends Engine implements IMainClasses, ILayouts {
    //Check which AndEngine scene is showing now and updates the new UI when setScene() is called.

    public Scenes currentScene;
    public boolean isGlobalManagerInit = false;

    public EngineBridge(EngineOptions pEngineOptions) {
        super(pEngineOptions);
    }

    @Override
    public void setScene(Scene pScene) {
        super.setScene(pScene);
        checkScene(pScene);
    }

    //--------------------------------------------------------------------------------------------//

    private void checkScene(Scene scene) {
        if (!isGlobalManagerInit) {
            initNewUi();
            return;
        }

        if (scene instanceof LoadingScreen.LoadingScene) {
            currentScene = Scenes.LOADING_SCREEN;
        }
        else if (scene instanceof MenuScene) {
            //PauseMenu is the only scene that extends MenuScene so this is a workaround for now.
            currentScene = Scenes.PAUSE_MENU;
        }
        else {
            for (Scenes toCheck: Scenes.values()) {
                if (toCheck.AndEngineScene != null && scene == toCheck.AndEngineScene) {
                    currentScene = toCheck;
                    break;
                }
            }
        }
        updateNewUi();
    }

    private void initNewUi() {
        TopBar.instance = new TopBar();
    }

    private void updateNewUi() {
        if (platform == null || currentScene == null)
            return;

        // This sets which layouts show according to the current scene.
        switch(currentScene) {

            case MAIN_MENU:
                topBar.show();
                platform.closeAllExcept(topBar);
                break;
            case LOADING_SCREEN:
            case SONG_MENU:
            case SCORING:
            case GAME:
                platform.closeAll();
                break;
        }

        // This updates the TopBar according to the current scene.
        // mActivity.runOnUiThread(() -> TopBar.instance.reload());
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
