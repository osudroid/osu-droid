package com.reco1l;

import android.util.Log;

import com.reco1l.andengine.ISceneHandler;
import com.reco1l.andengine.OsuScene;
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

    private static EngineMirror instance;

    public Scenes currentScene;
    public Scenes lastScene;

    private final List<ISceneHandler> sceneHandlers;

    //--------------------------------------------------------------------------------------------//

    public EngineMirror(EngineOptions pEngineOptions) {
        super(pEngineOptions);
        instance = this;
        sceneHandlers = new ArrayList<>();
    }

    public static EngineMirror getInstance() {
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void setScene(Scene scene) {
        if (isGlobalManagerInit) {
            this.lastScene = currentScene;
            this.currentScene = parseScene(scene);
            mActivity.runOnUiThread(this::updateUI);
        }
        super.setScene(scene);
    }

    private Scenes parseScene(Scene scene) {
        Scenes type = null;

        if (scene instanceof OsuScene) {
            type = ((OsuScene) scene).getIdentifier();
        }
        if (scene.hasChildScene() && scene.getChildScene() == PauseMenu.getInstance().getScene()) {
            type = Scenes.PAUSE;
        }
        else if (scene == LoadingScreen.getInstance().getScene() || scene == UI.loadingScene.scene) {
            type = Scenes.LOADING;
        }
        else if (scene == global.getScoring().getScene()) {
            type = Scenes.SCORING;
        }
        else if (scene == global.getGameScene().getScene()) {
            type = Scenes.GAME;
        }

        if (type != null) {
            if (lastScene != null) {
                Log.i("Engine", "Setting scene to " + type.name() + " last scene was " + lastScene.name());
            } else {
                Log.i("Engine", "Setting scene to " + type.name());
            }
        }
        return type;
    }

    //--------------------------------------------------------------------------------------------//

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

    //--------------------------------------------------------------------------------------------//

    public void registerSceneHandler(ISceneHandler sceneHandler) {
        this.sceneHandlers.add(sceneHandler);
    }
}
