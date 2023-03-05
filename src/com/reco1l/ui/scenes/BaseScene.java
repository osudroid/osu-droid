package com.reco1l.ui.scenes;

// Created by Reco1l on 26/9/22 13:38

import android.widget.LinearLayout;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.management.music.IMusicObserver;
import com.reco1l.management.resources.ResourceTable;
import com.reco1l.utils.Logging;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public abstract class BaseScene extends Scene implements IMusicObserver, ResourceTable {

    private boolean mIsFirstTimeShowing = true;

    //--------------------------------------------------------------------------------------------//

    public BaseScene() {
        Logging.initOf(getClass());
        Game.engine.onSceneCreated(this);

        registerUpdateHandler(new IUpdateHandler() {

            public void onUpdate(float pSecondsElapsed) {
                if (isShowing()) {
                    onSceneUpdate(pSecondsElapsed);
                }
            }

            public void reset() {}
        });

        Game.musicManager.bindMusicObserver(new IMusicObserver() {
            public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
                if (isShowing()) {
                    BaseScene.this.onMusicChange(newTrack, isSameAudio);
                }
            }

            public void onMusicPause() {
                if (isShowing()) {
                    BaseScene.this.onMusicPause();
                }
            }

            public void onMusicPlay() {
                if (isShowing()) {
                    BaseScene.this.onMusicPlay();
                }
            }

            public void onMusicStop() {
                if (isShowing()) {
                    BaseScene.this.onMusicStop();
                }
            }

            public void onMusicEnd() {
                if (isShowing()) {
                    BaseScene.this.onMusicEnd();
                }
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    protected void onFirstShow() {}

    protected void onSceneUpdate(float sec) {}

    public void onButtonContainerChange(LinearLayout layout) {}

    //--------------------------------------------------------------------------------------------//

    protected final boolean isShowing() {
        return Game.engine.getScene() == this;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
        if (newTrack != null) {
            UI.background.changeFrom(newTrack.getBackground());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean onBackPress() {
        return false;
    }

    public void onSceneChange(Scene lastScene, Scene newScene) {
        if (newScene != this) {
            return;
        }

        if (mIsFirstTimeShowing) {
            mIsFirstTimeShowing = false;
            onFirstShow();
        }
    }

    public void onPause() {}

    public void onResume() {}

    //--------------------------------------------------------------------------------------------//

    public void show() {
        Game.engine.setScene(this);
    }

    public void init() {}
}
