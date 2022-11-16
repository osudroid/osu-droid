package com.reco1l.andengine;

// Created by Reco1l on 26/9/22 13:38

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.game.TimingWrapper;
import com.reco1l.interfaces.IMusicObserver;
import com.reco1l.enums.MusicOption;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public abstract class BaseScene extends Scene implements ISceneHandler, IMusicObserver {

    public TimingWrapper timingWrapper;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    private boolean isTimingWrapperEnabled = false;
    private boolean isContinuousPlay = true;

    //--------------------------------------------------------------------------------------------//

    public BaseScene() {
        timingWrapper = new TimingWrapper();

        Game.engine.registerSceneHandler(this);
        onCreate();
        registerUpdateHandler(elapsed -> {
            if (isTimingWrapperEnabled) {
                int position = Game.songService.getPosition();

                if (Game.songService.getStatus() == Status.PLAYING) {
                    timingWrapper.update(elapsed, position);
                } else {
                    timingWrapper.setBeatLength(1000);
                    timingWrapper.update(elapsed, -1);
                }
            }
            onSceneUpdate(elapsed);
        });

        Game.musicManager.bindMusicObserver(this, this);
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract void onCreate();

    protected abstract void onSceneUpdate(float secondsElapsed);

    //--------------------------------------------------------------------------------------------//

    protected final void setTimingWrapper(boolean bool) {
        this.isTimingWrapperEnabled = bool;
    }

    protected final void setContinuousPlay(boolean bool) {
        this.isContinuousPlay = bool;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicControlRequest(MusicOption option, Status current) {
        if (!isTimingWrapperEnabled)
            return;

        if (option == MusicOption.PLAY) {
            if (current == Status.PAUSED) {
                timingWrapper.restoreBPMLength();
                timingWrapper.computeOffsetAtPosition(Game.songService.getPosition());
            }
        }

        if (option == MusicOption.PREVIOUS || option == MusicOption.NEXT) {
            timingWrapper.firstPoint = null;
        }
    }

    @Override
    public void onMusicControlChanged(MusicOption option, Status status) {
        if (isTimingWrapperEnabled) {
            if (option == MusicOption.STOP || option == MusicOption.PAUSE) {
                timingWrapper.setBeatLength(1000);
            }
        }
    }

    @Override
    public void onMusicChange(BeatmapInfo beatmap) {
        if (beatmap != null) {
            timingWrapper.loadPointsFrom(beatmap);

            if (timingWrapper.computeFirstBpmLength()) {
                timingWrapper.computeOffset();
            }

            String path = beatmap.getTrack(0).getBackground();
            UI.background.change(path);
        }
    }

    @Override
    public void onMusicSync(Status status) {
        if (isTimingWrapperEnabled) {
            timingWrapper.computeOffsetAtPosition(Game.songService.getPosition());
        }
    }

    @Override
    public void onMusicEnd() {
        if (isContinuousPlay) {
            Game.musicManager.next();
        }
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {

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
