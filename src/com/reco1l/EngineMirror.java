package com.reco1l;

import com.reco1l.andengine.ISceneHandler;
import com.reco1l.interfaces.IReferences;
import com.reco1l.enums.Scenes;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.PauseMenu;

// Created by Reco1l on 22/6/22 02:20

public class EngineMirror extends Engine implements IReferences {

    public static boolean isGlobalManagerInit = false;

    public Scenes currentScene;
    public Scenes lastScene;

    private final List<ISceneHandler> sceneHandlers;

    //--------------------------------------------------------------------------------------------//

    public EngineMirror(EngineOptions pEngineOptions) {
        super(pEngineOptions);
        this.sceneHandlers = new ArrayList<>();
    }

    @Override
    public void setScene(Scene scene) {
        if (isGlobalManagerInit) {
            this.lastScene = currentScene;
            this.currentScene = parseScene(scene);
            mActivity.runOnUiThread(this::updateUI);
        }
        super.setScene(scene);
    }

    //--------------------------------------------------------------------------------------------//

    private Scenes parseScene(Scene scene) {
        if (scene.hasChildScene() && scene.getChildScene() == PauseMenu.getInstance().getScene()) {
            return Scenes.PAUSE;
        }
        else if (scene == LoadingScreen.getInstance().getScene() || scene == UI.loadingScene.scene) {
            return Scenes.LOADING;
        }
        else if (scene == global.getMainScene()) {
            return Scenes.MAIN;
        }
        else if (scene == global.getSongMenu()) {
            return Scenes.LIST;
        }
        else if (scene == global.getScoring().getScene()) {
            return Scenes.SCORING;
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        for (ISceneHandler handler : sceneHandlers) {
            if (currentScene == handler.getIdentifier()) {
                handler.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (ISceneHandler handler : sceneHandlers) {
            if (currentScene == handler.getIdentifier()) {
                handler.onPause();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void updateUI() {
        if (currentScene == null)
            return;

        platform.close(UI.getExtras());
        platform.closeAllExcept(platform.getFragmentsFrom(currentScene));
        platform.showAll(currentScene);

        UI.notificationCenter.allowBadgeNotificator(currentScene == Scenes.GAME);

        platform.notifySceneChange(lastScene, currentScene);
    }

    public void registerSceneHandler(ISceneHandler sceneHandler) {
        this.sceneHandlers.add(sceneHandler);
    }
}
