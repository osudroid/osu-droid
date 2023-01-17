package com.reco1l;

import com.reco1l.interfaces.SceneHandler;
import com.reco1l.scenes.BaseScene;
import com.reco1l.enums.Screens;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.util.FPSCounter;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 22/6/22 02:20

public final class GameEngine extends Engine {

    private static GameEngine instance;

    private final List<SceneHandler> handlers;

    private Scene lastScene;
    private Screens currentScreen, lastScreen;

    private boolean isGameLoaded = false;

    private float
            fps,
            frameTime;

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

    public float getFPS() {
        return fps;
    }

    public float getFrameTime() {
        return frameTime;
    }

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

        registerUpdateHandler(new FPSCounter() {
            @Override
            public void onUpdate(float pSecondsElapsed) {
                super.onUpdate(pSecondsElapsed);
                frameTime = pSecondsElapsed * 1000;
                fps = getFPS();
            }
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

            Game.platform.onScreenChange(lastScreen, currentScreen);
        }
        lastScene = getScene();
        super.setScene(scene);

        synchronized (handlers) {
            handlers.forEach(handler -> handler.onSceneChange(lastScene, scene));
        }
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

        synchronized (handlers) {
            handlers.forEach(handler -> {
                if (currentScreen == handler.getIdentifier()) {
                    handler.onResume();
                }
            });
        }
        Game.timingWrapper.sync();
    }

    @Override
    public void onPause() {
        super.onPause();

        synchronized (handlers) {
            handlers.forEach(handler -> {
                if (currentScreen == handler.getIdentifier()) {
                    handler.onPause();
                }
            });
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void registerSceneHandler(SceneHandler sceneHandler) {
        synchronized (handlers) {
            this.handlers.add(sceneHandler);
        }
    }

    public SceneHandler getCurrentSceneHandler() {
        synchronized (handlers) {
            for (SceneHandler handler : handlers) {
                if (handler.getIdentifier() == currentScreen) {
                    return handler;
                }
            }
        }
        return null;
    }
}
