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

    protected final Context context;

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

    protected boolean isShowing() {
        return Game.engine.getScene() == this;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getAttachedScreen() {
        return getIdentifier();
    }

    @Override
    public void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        if (!isShowing()) {
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
        if (!isShowing()) {
            return;
        }

        if (isContinuousPlay) {
            Game.musicManager.next();
        }
    }

    @Override
    public boolean onBackPress() {
        return Game.engine.backToLastScene();
    }

    public void show() {
        Game.engine.setScene(this);
    }
}
