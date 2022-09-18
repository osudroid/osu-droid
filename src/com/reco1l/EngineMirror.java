package com.reco1l;

import com.reco1l.ui.platform.UI;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.interfaces.IMainClasses;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;

// Created by Reco1l on 22/6/22 02:20

public class EngineMirror extends Engine implements IMainClasses, UI {

    public static boolean isGlobalManagerInit = false;

    public Scenes currentScene;
    public Scenes lastScene;

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
        lastScene = currentScene;

        /*if (scene.hasChildScene() && scene.getChildScene() == PauseMenu.getInstance().getScene()) {
            currentScene = Scenes.PAUSE_MENU;
        }*/
        if (scene == LoadingScreen.getInstance().getScene() || scene == loadingScene.scene) {
            currentScene = Scenes.LOADING_SCENE;
        }
        else if (scene == global.getMainScene().getScene()) {
            currentScene = Scenes.MAIN_SCENE;
        }
        else if (scene == global.getSongMenu().getScene()) {
            currentScene = Scenes.SONG_MENU;
        }
        else if (scene == global.getScoring().getScene()) {
            currentScene = Scenes.SCORING;
        }
        mActivity.runOnUiThread(this::updateUI);
    }

    /**
     * Updates the fragments based UI when {@link #setScene(Scene)} is called.
     */
    private void updateUI() {
        if (currentScene == null || !UIManager.isUserInterfaceInit)
            return;

        platform.close(UIManager.getExtras());
        platform.closeAllExcept(platform.getFragmentsFrom(currentScene));
        platform.showAll(currentScene);

        notificationCenter.allowBadgeNotificator(currentScene == Scenes.GAME);

        platform.updateFragments(lastScene, currentScene);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onPause() {
        platform.handleWindowFocus(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        platform.handleWindowFocus(true);
    }
}
