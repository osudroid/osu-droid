package com.reco1l;

import com.reco1l.andengine.ISceneHandler;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 22/6/22 02:20

public class GameEngine extends Engine {

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

        registerUpdateHandler(Game.timingWrapper::onUpdate);
    }

    public static GameEngine getInstance() {
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void notifyLoadCompleted() {
        isGlobalInitialized = true;
        registerUpdateHandler(Game.platform::onUpdate);
    }

    @Override
    public void setScene(Scene scene) {
        if (isGlobalInitialized) {
            lastScreen = currentScreen;
            currentScreen = parseScreen(scene);
            Game.activity.runOnUiThread(() -> notifyUI(lastScreen));
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
        Game.timingWrapper.sync();
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

        Game.platform.close(UI.getExtras());
        Game.platform.closeAllExcept(Game.platform.getFragmentsFrom(currentScreen));
        Game.platform.showAll(currentScreen);

        UI.notificationCenter.allowPopupNotifications(currentScreen != Screens.Game);

        Game.platform.notifyScreenChange(oldScene, currentScreen);
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

    public Screens getCurrentScreen() {
        return currentScreen;
    }
}
