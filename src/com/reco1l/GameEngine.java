package com.reco1l;

import com.reco1l.andengine.ISceneHandler;
import com.reco1l.andengine.BaseScene;
import com.reco1l.interfaces.IReferences;
import com.reco1l.enums.Screens;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 22/6/22 02:20

public class GameEngine extends Engine implements IReferences {

    private static GameEngine instance;

    public Screens currentScreen, lastScreen;
    public Scene lastScene;

    public boolean isGlobalInitialized = false;

    private final List<ISceneHandler> sceneHandlers;

    //--------------------------------------------------------------------------------------------//

    public GameEngine(EngineOptions pEngineOptions) {
        super(pEngineOptions);
        instance = this;
        sceneHandlers = new ArrayList<>();
    }

    public static GameEngine getInstance() {
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void notifyGlobalInit() {
        isGlobalInitialized = true;
        registerUpdateHandler(Game.platform::onUpdate);
    }

    @Override
    public void setScene(Scene scene) {
        if (isGlobalInitialized) {
            lastScreen = currentScreen;
            currentScreen = parseScreen(scene);
            activity.runOnUiThread(() -> notifyUI(lastScreen));
        }
        lastScene = getScene();
        super.setScene(scene);

        for (ISceneHandler handler : sceneHandlers) {
            handler.onSceneChange(lastScene, scene);
        }
    }

    private Screens parseScreen(Scene scene) {
        Screens type = null;

        if (scene instanceof BaseScene) {
            type = ((BaseScene) scene).getIdentifier();
        }
        else if (scene == Game.gameScene.getScene()) {
            type = Screens.Game;
        }
        return type;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onResume() {
        super.onResume();
        for (ISceneHandler handler : sceneHandlers) {
            if (currentScreen == handler.getIdentifier()) {
                handler.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        for (ISceneHandler handler : sceneHandlers) {
            if (currentScreen == handler.getIdentifier()) {
                handler.onPause();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void notifyUI(Screens oldScene) {
        if (currentScreen == null)
            return;

        platform.close(UI.getExtras());
        platform.closeAllExcept(platform.getFragmentsFrom(currentScreen));
        platform.showAll(currentScreen);

        UI.notificationCenter.allowBadgeNotificator(currentScreen == Screens.Game);

        platform.notifyScreenChange(oldScene, currentScreen);
    }

    //--------------------------------------------------------------------------------------------//

    public void registerSceneHandler(ISceneHandler sceneHandler) {
        this.sceneHandlers.add(sceneHandler);
    }

    public ISceneHandler getCurrentSceneHandler() {
        for (ISceneHandler handler : sceneHandlers) {
            if (handler.getIdentifier() == currentScreen) {
                return handler;
            }
        }
        return null;
    }

    public BaseScene getCurrentScene() {
        if (getScene() instanceof BaseScene) {
            return (BaseScene) getScene();
        }
        return null;
    }
}
