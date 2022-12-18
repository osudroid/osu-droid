package com.reco1l.andengine;

// Created by Reco1l on 26/9/22 13:38

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.interfaces.IMusicObserver;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public abstract class BaseScene extends Scene implements ISceneHandler, IMusicObserver {

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    private boolean
            isBackgroundAutoChange = true,
            isContinuousPlay = true;

    //--------------------------------------------------------------------------------------------//

    public BaseScene() {

        Game.engine.registerSceneHandler(this);
        onCreate();
        registerUpdateHandler(elapsed -> {
            onSceneUpdate(elapsed);
        });

        Game.musicManager.bindMusicObserver(this);
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract void onCreate();

    protected abstract void onSceneUpdate(float secondsElapsed);

    //--------------------------------------------------------------------------------------------//

    protected final void setContinuousPlay(boolean bool) {
        this.isContinuousPlay = bool;
    }

    protected final void setBackgroundAutoChange(boolean bool) {
        this.isBackgroundAutoChange = bool;
    }

    //--------------------------------------------------------------------------------------------//


    @Override
    public Screens getAttachedScreen() {
        return getIdentifier();
    }

    @Override
    public void onMusicChange(BeatmapInfo beatmap) {
        if (Game.engine.getScene() != this)
            return;

        if (isBackgroundAutoChange) {
            if (beatmap != null) {
                String path = beatmap.getTrack(0).getBackground();
                UI.background.changeFrom(path);
            }
        }
    }

    @Override
    public void onMusicEnd() {
        if (isContinuousPlay) {
            Game.musicManager.next();
        }
    }

    @Override
    public boolean onBackPress() {
        if (Game.engine.lastScene != null) {
            Game.engine.setScene(Game.engine.lastScene);
        }
        return true;
    }

    public void show() {
        Game.engine.setScene(this);
    }
}
