package com.reco1l;

import com.reco1l.ui.Inbox;
import com.reco1l.ui.TopBar;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.UI;
import com.reco1l.utils.IMainClasses;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;

// Created by Reco1l on 22/6/22 02:20

public class EngineMirror extends Engine implements IMainClasses, UI {
    // Checks which AndEngine scene is showing now and updates the new UI when setScene() is called.

    public Scenes currentScene;
    public static boolean isGlobalManagerInit = false;

    private final BaseLayout[] extras = {
            UI.inbox,
    };

    public EngineMirror(EngineOptions pEngineOptions) {
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
            global.setInfo("Loading UI...");
            initializeUI();
            return;
        }

        for (Scenes toCheck : Scenes.values()) {
            if (toCheck.AndEngineScene == null)
                continue;

            if (scene.hasChildScene() && scene.getChildScene() == toCheck.AndEngineScene) {
                currentScene = toCheck;
                break;
            }
            if (scene == toCheck.AndEngineScene) {
                currentScene = toCheck;
                break;
            }
        }
        updateUI();
    }

    private void initializeUI() {
        TopBar.instance = new TopBar();
        Inbox.instance = new Inbox();
        Inbox.notifications = new ArrayList<>();
    }

    /**
     * Updates the fragments based UI when {@link #setScene(Scene)} is called.
     */
    private void updateUI() {
        if (platform == null || currentScene == null)
            return;

        // Closing all extras on every scene change.
        platform.closeThis(extras);

        // This sets which layouts show according to the current scene.
        switch(currentScene) {

            case LOADING_SCREEN:
                inbox.allowBadgeNotificator(true);
                break;
            case MAIN_MENU:
                topBar.show();
                platform.closeAllExcept(topBar);
                inbox.allowBadgeNotificator(true);
                break;
            case SONG_MENU:
            case SCORING:
            case GAME:
                inbox.allowBadgeNotificator(false);
                platform.closeAll();
                break;
        }

        // This updates the TopBar according to the current scene.
        // mActivity.runOnUiThread(() -> TopBar.instance.reload());
    }

    public enum Scenes {
        PAUSE_MENU(PauseMenu.getInstance().getScene()),
        LOADING_SCREEN(LoadingScreen.getInstance().getScene()),
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
