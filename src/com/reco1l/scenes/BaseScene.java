package com.reco1l.scenes;

// Created by Reco1l on 26/9/22 13:38

import android.content.Context;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.interfaces.IBaseScene;
import com.reco1l.interfaces.MusicObserver;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public abstract class BaseScene extends Scene implements IBaseScene, MusicObserver {

    protected Context context;

    private boolean
            isBackgroundAutoChange = true,
            isContinuousPlay = true;

    //--------------------------------------------------------------------------------------------//

    public BaseScene() {
        context = Game.activity;

        Game.engine.registerSceneHandler(this);
        Game.musicManager.bindMusicObserver(this);
        onCreate();
        registerUpdateHandler(this::onSceneUpdate);
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
    public void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        if (Game.engine.getScene() != this) {
            return;
        }

        if (isBackgroundAutoChange) {
            if (track != null) {
                UI.background.changeFrom(track.getBackground());
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
