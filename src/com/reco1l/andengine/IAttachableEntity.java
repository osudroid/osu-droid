package com.reco1l.andengine;

// Created by Reco1l on 18/9/22 19:36

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;

public interface IAttachableEntity {

    int screenWidth = Config.getRES_WIDTH();
    int screenHeight = Config.getRES_HEIGHT();

    void draw(Scene scene, int index);

    default void update() {}
}
