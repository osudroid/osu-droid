package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import static com.reco1l.enums.MusicOption.*;

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.andengine.ISceneHandler;
import com.reco1l.interfaces.IMusicObserver;
import com.reco1l.enums.MusicOption;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public final class MusicManager {

    public BeatmapInfo beatmap;

    private static MusicManager instance;

    private final Map<Screens, IMusicObserver> observers;

    //--------------------------------------------------------------------------------------------//

    public MusicManager() {
        this.observers = new HashMap<>();
    }

    //--------------------------------------------------------------------------------------------//

    public static MusicManager getInstance() {
        if (instance == null) {
            instance = new MusicManager();
        }
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isInvalidRequest(Status... toCheck) {
        if (getService() == null) {
            Log.e("MusicManager", "InvalidRequest: SongService is not initialized!");
            return true;
        }
        if (toCheck.length > 0) {
            for (Status status : toCheck) {
                if (getState() == status) {
                    return false;
                }
            }
            Log.e("MusicManager", "InvalidRequest: cannot call this action while state is " + getState().name());
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    private Status getState() {
        return Game.songService.getStatus();
    }

    public SongService getService() {
        return Game.songService;
    }

    public IMusicObserver getCurrentObserver() {
        return observers.get(Game.engine.currentScreen);
    }

    //--------------------------------------------------------------------------------------------//

    public void bindMusicObserver(ISceneHandler handledScene, IMusicObserver observer) {
        this.observers.put(handledScene.getIdentifier(), observer);
    }

    private void notifyControl(MusicOption option) {
        Log.i("MusicManager", "Option " + option.name() + " was received");
        if (getCurrentObserver() != null) {
            getCurrentObserver().onMusicControlRequest(option, getState());
        }
    }

    private void notifyStateChange(MusicOption option) {
        if (getCurrentObserver() != null) {
            getCurrentObserver().onMusicControlChanged(option, getState());
        }
    }

    private void notifyMusicChange(BeatmapInfo newBeatmap) {
        if (getCurrentObserver() != null) {
            getCurrentObserver().onMusicChange(newBeatmap);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void change(BeatmapInfo beatmap) {
        if (isInvalidRequest() || beatmap == null)
            return;

        if (getState() != Status.STOPPED) {
            getService().stop();
        }
        this.beatmap = beatmap;
        play();
    }

    public void play() {
        if (isInvalidRequest(Status.PAUSED, Status.STOPPED))
            return;

        notifyControl(PLAY);

        boolean wasMusicChanged = false;
        if (beatmap != null && getState() == Status.STOPPED) {
            wasMusicChanged = getService().preLoad(beatmap.getMusic());
        }

        getService().play();
        getService().setVolume(Config.getBgmVolume());

        if (wasMusicChanged) {
            notifyMusicChange(beatmap);
        }
        notifyStateChange(PLAY);
    }

    public void pause() {
        if (isInvalidRequest(Status.PLAYING))
            return;

        notifyControl(PAUSE);
        getService().pause();
        notifyStateChange(PAUSE);
    }

    public void stop() {
        if (isInvalidRequest(Status.PLAYING, Status.PAUSED))
            return;

        notifyControl(STOP);
        getService().stop();
        notifyStateChange(STOP);
    }

    public void previous() {
        if (isInvalidRequest())
            return;

        notifyControl(PREVIOUS);
        if (getState() == Status.PLAYING || getState() == Status.PAUSED) {
            getService().stop();
        }
        beatmap = Game.library.getPrevBeatmap();
        getService().preLoad(beatmap.getMusic());
        getService().play();
        notifyMusicChange(beatmap);
        getService().setVolume(Config.getBgmVolume());
        notifyStateChange(PREVIOUS);
    }

    public void next() {
        notifyControl(NEXT);
        if (getState() == Status.PLAYING || getState() == Status.PAUSED) {
            getService().stop();
        }
        beatmap = Game.library.getNextBeatmap();
        getService().preLoad(beatmap.getMusic());
        getService().play();
        notifyMusicChange(beatmap);
        getService().setVolume(Config.getBgmVolume());
        notifyStateChange(NEXT);
    }

    public void sync() {
        if (isInvalidRequest(Status.PLAYING))
            return;

        if (getCurrentObserver() != null) {
            getCurrentObserver().onMusicSync(getState());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicEnd() {
        if (getCurrentObserver() != null) {
            getCurrentObserver().onMusicEnd();
        }
    }

}
