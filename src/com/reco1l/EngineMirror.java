package com.reco1l;

import com.reco1l.ui.data.Notificator;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.interfaces.UI;
import com.reco1l.utils.interfaces.IMainClasses;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;

// Created by Reco1l on 22/6/22 02:20

public class EngineMirror extends Engine implements IMainClasses, UI {
    // Checks which AndEngine scene is showing now and updates the new UI when setScene() is called.

    public Scenes currentScene;
    public static boolean isGlobalManagerInit = false;

    public enum Scenes {
        LOADING_SCREEN,
        PAUSE_MENU,
        MAIN_MENU,
        SONG_MENU,
        SCORING,
        GAME
    }

    //--------------------------------------------------------------------------------------------//

    public EngineMirror(EngineOptions pEngineOptions) {
        super(pEngineOptions);
    }

    @Override
    public void setScene(Scene scene) {
        compare(scene);
        super.setScene(scene);
    }

    //--------------------------------------------------------------------------------------------//

    private void compare(Scene scene) {
        if (!isGlobalManagerInit) {
            UIManager.initialize();
            return;
        }

        /*if (scene.hasChildScene() && scene.getChildScene() == PauseMenu.getInstance().getScene()) {
            currentScene = Scenes.PAUSE_MENU;
        }*/

        if (scene == LoadingScreen.getInstance().getScene()) {
            currentScene = Scenes.LOADING_SCREEN;
        }
        if (scene == global.getMainScene().getScene()) {
            currentScene = Scenes.MAIN_MENU;
        }
        if (scene == global.getSongMenu().getScene()) {
            currentScene = Scenes.SONG_MENU;
        }
        if (scene == global.getScoring().getScene()) {
            currentScene = Scenes.SCORING;
        }
        if (scene == global.getGameScene().getScene()) {
            currentScene = Scenes.GAME;
        }

        mActivity.runOnUiThread(this::updateUI);
    }

    /**
     * Updates the fragments based UI when {@link #setScene(Scene)} is called.
     */
    private void updateUI() {
        if (currentScene == null || !UIManager.isUserInterfaceInit)
            return;

        Notificator.debug("Current scene: " + currentScene.name());

        // Closing all extras on every scene change.
        platform.closeThis(UIManager.extras);

        // This sets which layouts show according to the current scene.
        switch (currentScene) {

            case LOADING_SCREEN:
                inbox.allowBadgeNotificator(true);
                break;
            case MAIN_MENU:
                topBar.show();
                platform.closeAllExcept(topBar);
                inbox.allowBadgeNotificator(true);
                break;
            case SONG_MENU:
                break;
            case SCORING:
            case GAME:
                inbox.allowBadgeNotificator(false);
                platform.closeAll();
                break;
        }

        // This updates the TopBar according to the current scene.
        topBar.reload();
    }
}
