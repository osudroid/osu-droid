package com.reco1l.andengine;

// Created by Reco1l on 18/9/22 20:50

import com.reco1l.enums.Scenes;

public interface ISceneHandler {

    Scenes getIdentifier();

    default void onPause() {}
    default void onResume() {}
}
