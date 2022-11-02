package com.reco1l.andengine;

// Created by Reco1l on 26/9/22 13:38

import com.reco1l.Game;
import com.reco1l.andengine.entity.Background;
import com.reco1l.game.TimingWrapper;
import com.reco1l.interfaces.IMusicObserver;
import com.reco1l.enums.MusicOption;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public abstract class OsuScene extends Scene implements ISceneHandler, IMusicObserver {

    public Background background;

    public TimingWrapper timingWrapper;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    private boolean isUsingTimingWrapper = false;
    private boolean isContinuousPlay = true;

    //--------------------------------------------------------------------------------------------//

    public OsuScene() {
        timingWrapper = new TimingWrapper();

        background = new Background();
        background.draw(this, 2);

        Game.engine.registerSceneHandler(this);
        onCreate();
        registerUpdateHandler(elapsed -> {
            if (isUsingTimingWrapper) {
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

    public void onSceneShow(Scene lastScene) {}

    //--------------------------------------------------------------------------------------------//

    protected final void setTimingWrapper(boolean bool) {
        this.isUsingTimingWrapper = bool;
    }

    protected final void setContinuousPlay(boolean bool) {
        this.isContinuousPlay = bool;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicControlRequest(MusicOption option, Status current) {
        if (!isUsingTimingWrapper)
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
        if (isUsingTimingWrapper) {
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
            background.change(path);
        }
    }

    @Override
    public void onMusicSync(Status status) {
        if (isUsingTimingWrapper) {
            timingWrapper.computeOffsetAtPosition(Game.songService.getPosition());
        }
    }

    @Override
    public void onMusicEnd() {
        if (isContinuousPlay) {
            Game.musicManager.next();
        }
    }
}
