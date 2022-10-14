package com.reco1l.andengine;

// Created by Reco1l on 26/9/22 13:38

import com.reco1l.Game;
import com.reco1l.andengine.entity.Background;
import com.reco1l.game.TimingWrapper;
import com.reco1l.interfaces.IMusicObserver;
import com.reco1l.management.MusicManager;
import com.reco1l.enums.MusicOption;
import com.reco1l.utils.AsyncExec;

import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;

public abstract class OsuScene extends Scene implements ISceneHandler, IMusicObserver {

    public Background background;

    protected TimingWrapper timingWrapper;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    private boolean isUsingTimingWrapper = false;
    private boolean isContinuousPlay = true;

    //--------------------------------------------------------------------------------------------//

    public OsuScene() {
        this.background = new Background();
        this.timingWrapper = new TimingWrapper();

        this.background.draw(this, 2);

        Game.engine.registerSceneHandler(this);
        onCreate();
        registerUpdateHandler(elapsed -> {
            if (isUsingTimingWrapper) {
                int position = getSongService().getPosition();

                if (getSongService().getStatus() == Status.PLAYING) {
                    timingWrapper.update(elapsed, position);
                } else {
                    timingWrapper.setBPMLength(1000);
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

    protected void setTimingWrapper(boolean bool) {
        this.isUsingTimingWrapper = bool;
    }

    protected void setContinuousPlay(boolean bool) {
        this.isContinuousPlay = bool;
    }

    //--------------------------------------------------------------------------------------------//

    protected SongService getSongService() {
        return Game.global.getSongService();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicControlRequest(MusicOption option, Status current) {
        if (!isUsingTimingWrapper)
            return;

        if (option == MusicOption.PLAY) {
            if (current == Status.PAUSED) {
                timingWrapper.restoreBPMLength();
                timingWrapper.computeOffsetAtPosition(Game.global.getSongService().getPosition());
            }
        }

        if (option == MusicOption.PREVIOUS || option == MusicOption.NEXT) {
            timingWrapper.firstPoint = null;
        }
    }

    @Override
    public void onMusicControlChanged(MusicOption option, Status status) {
        if (!isUsingTimingWrapper)
            return;

        if (option == MusicOption.PLAY) {
            if (status == Status.STOPPED) {
                timingWrapper.loadPointsFrom(MusicManager.beatmap);

                if (timingWrapper.computeFirstBpmLength()) {
                    timingWrapper.computeOffset();
                }
            }
        }

        if (option == MusicOption.PREVIOUS || option == MusicOption.NEXT) {
            timingWrapper.loadPointsFrom(MusicManager.beatmap);

            String path = MusicManager.beatmap.getTrack(0).getBackground();
            background.sprite.setColor(0, 0, 0);

            new AsyncExec() {
                TextureRegion texture;

                public void run() {
                    texture = Game.resources.loadBackground(path);
                }
                public void onComplete() {
                    background.change(texture);
                }
            }.execute();
        }

        if (option == MusicOption.STOP || option == MusicOption.PAUSE) {
            timingWrapper.setBPMLength(1000);
        }
    }

    @Override
    public void onMusicSync(Status status) {
        if (isUsingTimingWrapper) {
            timingWrapper.computeOffsetAtPosition(Game.global.getSongService().getPosition());
        }
    }

    @Override
    public void onMusicEnd() {
        if (isContinuousPlay) {
            Game.musicManager.next();
        }
    }
}
