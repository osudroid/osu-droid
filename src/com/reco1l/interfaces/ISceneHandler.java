package com.reco1l.interfaces;

// Created by Reco1l on 18/9/22 20:50

import com.reco1l.annotation.Legacy;

import org.anddev.andengine.entity.scene.Scene;

@Legacy // This can be removed if we make GameScene extend BaseScene
public interface ISceneHandler {

    default void onPause() {}

    default void onResume() {}

    default void onSceneChange(Scene oldScene, Scene newScene) {}

    default boolean onBackPress() {
        return false;
    }
}
