package com.reco1l;

import com.reco1l.interfaces.IBaseScene;
import com.reco1l.scenes.BaseScene;
import com.reco1l.enums.Screens;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 22/6/22 02:20

public final class GameEngine extends Engine {

    private static GameEngine instance;

    private final List<IBaseScene> handlers;

    private Scene lastScene;
    private Screens currentScreen, lastScreen;

    private boolean isGameLoaded = false;

    //--------------------------------------------------------------------------------------------//

    public GameEngine(EngineOptions pEngineOptions) {
        super(pEngineOptions);
        instance = this;
        handlers = new ArrayList<>();
    }

    public static GameEngine getInstance() {
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public Screens getScreen() {
        return currentScreen;
    }

    public Screens getLastScreen() {
        return lastScreen;
    }

    public Scene getLastScene() {
        return lastScene;
    }

    public boolean isGameLoaded() {
        return isGameLoaded;
    }

    //--------------------------------------------------------------------------------------------//

    public void onLoadComplete() {
        isGameLoaded = true;

        registerUpdateHandler(sec -> {
            Game.platform.onEngineUpdate(sec);
            Game.timingWrapper.onUpdate(sec);
        });
    }

    public boolean backToLastScene() {
        if (lastScene != null) {
            setScene(lastScene);
            return true;
        }
        return false;
    }

    @Override
    public void setScene(Scene scene) {
        if (isGameLoaded) {
            lastScreen = getScreen();
            currentScreen = parseScreen(scene);

            Game.activity.runOnUiThread(() ->
                    Game.platform.onScreenChange(lastScreen, currentScreen)
            );
        }
        lastScene = getScene();
        super.setScene(scene);

        handlers.forEach(handler -> handler.onSceneChange(lastScene, scene));
    }

    private Screens parseScreen(Scene scene) {

        if (scene == Game.gameScene.getScene()) {
            return Screens.Game;
        }

        if (scene instanceof BaseScene) {
            return ((BaseScene) scene).getIdentifier();
        }
        return null;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onResume() {
        super.onResume();

        handlers.forEach(handler -> {
            if (currentScreen == handler.getIdentifier()) {
                handler.onResume();
            }
        });
        Game.timingWrapper.sync();
    }

    @Override
    public void onPause() {
        super.onPause();

        handlers.forEach(handler -> {
            if (currentScreen == handler.getIdentifier()) {
                handler.onPause();
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    public void registerSceneHandler(IBaseScene sceneHandler) {
        this.handlers.add(sceneHandler);
    }

    public IBaseScene getCurrentSceneHandler() {
        for (IBaseScene handler : handlers) {
            if (handler.getIdentifier() == currentScreen) {
                return handler;
            }
        }
        return null;
    }
}
