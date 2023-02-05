package com.reco1l;

import android.util.Log;

import com.reco1l.global.Game;
import com.reco1l.interfaces.ISceneHandler;
import com.reco1l.scenes.BaseScene;
import com.reco1l.utils.Logging;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.util.constants.TimeConstants;

import java.util.HashMap;
import java.util.Map;

// Created by Reco1l on 22/6/22 02:20

public final class GameEngine extends Engine {

    public static GameEngine instance;

    private final Map<Scene, ISceneHandler> mHandlers;

    private BaseScene
            mCurrentScene,
            mLastScene;

    private boolean mCanUpdate = false;

    //--------------------------------------------------------------------------------------------//

    public GameEngine(EngineOptions pEngineOptions) {
        super(pEngineOptions);
        Logging.initOf(getClass());

        instance = this;
        mHandlers = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//

    public void allowUpdate() {
        mCanUpdate = true;
    }

    @Override
    protected void onUpdate(long ns) throws InterruptedException {
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

        synchronized (mHandlers) {
            ISceneHandler h = mHandlers.get(mCurrentScene);

            if (h != null) {
                h.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mCanUpdate) {
            return;
        }

        synchronized (mHandlers) {
            ISceneHandler h = mHandlers.get(mCurrentScene);

            if (h != null) {
                h.onPause();
            }
        }
    }

    public boolean onBackPress() {
        if (mCurrentScene != null) {
            BaseScene scene = (BaseScene) mCurrentScene;

            if (scene.onBackPress()) {
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
            throw new RuntimeException("New scene cannot be null!");
        }

        if (newScene == mCurrentScene) {
            return;
        }
        Log.i("GameEngine", "Changing scene to " + newScene.getClass().getSimpleName());

        if (newScene instanceof BaseScene) {
            mLastScene = (BaseScene) getScene();
            mCurrentScene = (BaseScene) newScene;

            if (mCanUpdate) {
                Game.platform.onSceneChange(mLastScene, mCurrentScene);
            }
            super.setScene(newScene);
            mCurrentScene.onShow();

            synchronized (mHandlers) {
                ISceneHandler h = mHandlers.get(mCurrentScene);

                if (h != null) {
                    h.onSceneChange(mLastScene, newScene);
                }
            }
        } else {
            throw new RuntimeException("This engine only allow BaseScene types!");
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void registerSceneHandler(Scene scene, ISceneHandler sceneHandler) {
        synchronized (mHandlers) {
            this.mHandlers.put(scene, sceneHandler);
        }
    }
}
