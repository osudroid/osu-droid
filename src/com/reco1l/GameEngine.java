package com.reco1l;

import android.util.Log;

import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.tools.Logging;

import org.anddev.andengine.engine.LimitedFPSEngine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.constants.TimeConstants;

import java.util.ArrayList;

// Created by Reco1l on 22/6/22 02:20

public final class GameEngine extends LimitedFPSEngine {

    public static GameEngine instance;

    private final ArrayList<BaseScene> mScenes;

    private BaseScene
            mCurrentScene,
            mLastScene;

    private boolean mCanUpdate = false;

    //--------------------------------------------------------------------------------------------//

    public GameEngine(EngineOptions pEngineOptions) {
        // TODO [GameEngine] Variable frame rate
        this(pEngineOptions, 1000);
    }

    public GameEngine(EngineOptions pEngineOptions, int pFramesPerSecond) {
        super(pEngineOptions, pFramesPerSecond);
        instance = this;

        Logging.initOf(getClass());
        mScenes = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    public void allowUpdate() {
        mCanUpdate = true;
    }

    @Override
    public void onUpdate(long ns) throws InterruptedException {
        super.onUpdate(ns);

        if (mCanUpdate) {
            float sec = (float) ns / TimeConstants.NANOSECONDSPERSECOND;

            Game.platform.onEngineUpdate(sec);
            Game.timingWrapper.onUpdate(sec);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mCanUpdate) {
            return;
        }
        Game.timingWrapper.sync();

        if (mCurrentScene != null) {
            mCurrentScene.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mCanUpdate) {
            return;
        }

        if (mCurrentScene != null) {
            mCurrentScene.onPause();
        }
    }

    public void onWindowFocusChange(boolean isFocus) {
        if (!mCanUpdate) {
            return;
        }

        if (mCurrentScene != null) {
            mCurrentScene.onWindowFocusChange(isFocus);
        }
    }

    public boolean onBackPress() {
        if (mCurrentScene != null) {
            if (mCurrentScene.onBackPress()) {
                return true;
            }
        }
        return backScene();
    }

    //--------------------------------------------------------------------------------------------//

    public BaseScene getCurrent() {
        return mCurrentScene;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean backScene() {
        if (mLastScene == null) {
            return false;
        }
        setScene(mLastScene);
        return true;
    }

    @Override
    public void setScene(Scene newScene) {
        if (newScene == null) {
            throw new NullPointerException("New scene cannot be null!");
        }

        Log.i("GameEngine", "Changing scene to " + newScene.getClass().getSimpleName());

        if (newScene instanceof BaseScene) {
            if (mCurrentScene != newScene) {
                mLastScene = (BaseScene) getScene();
                mCurrentScene = (BaseScene) newScene;
            }

            if (mCanUpdate) {
                Game.platform.onSceneChange(mLastScene, mCurrentScene);
            }
            super.setScene(newScene);

            synchronized (mScenes) {
                mScenes.forEach(scene ->
                        scene.onSceneChange(mLastScene, mCurrentScene)
                );
            }
        } else {
            throw new RuntimeException("This engine only allow BaseScene types!");
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onSceneCreated(BaseScene scene) {
        mScenes.add(scene);
    }

}
