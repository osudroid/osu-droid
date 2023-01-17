package com.reco1l.scenes;

// Created by Reco1l on 26/9/22 13:38

import android.content.Context;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.interfaces.SceneHandler;
import com.reco1l.interfaces.MusicObserver;
import com.reco1l.tables.ResourceTable;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public abstract class BaseScene extends Scene implements SceneHandler, MusicObserver, ResourceTable {

    protected final Context context;

    private boolean
            isBackgroundAutoChange = true,
            isFirstTimeShowing = true,
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

    protected void onFirstShow() {}

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
    public void onMusicChange(TrackInfo pNewTrack, boolean pWasAudioChanged) {
        if (!isShowing()) {
            return;
        }

        if (isBackgroundAutoChange) {
            if (pNewTrack != null) {
                UI.background.changeFrom(pNewTrack.getBackground());
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

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        if (newScene == this && isFirstTimeShowing) {
            isFirstTimeShowing = false;
            onFirstShow();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void show() {
        Game.engine.setScene(this);
    }
}
