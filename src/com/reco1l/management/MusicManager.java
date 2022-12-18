package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import static com.reco1l.enums.MusicOption.*;

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.interfaces.IMusicObserver;
import com.reco1l.enums.MusicOption;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public final class MusicManager {

    private static MusicManager instance;

    private final ArrayList<IMusicObserver> observers;

    private BeatmapInfo beatmap;

    //--------------------------------------------------------------------------------------------//

    public MusicManager() {
        this.observers = new ArrayList<>();
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

    public boolean isPlaying() {
        return Game.songService != null && Game.songService.getStatus() == Status.PLAYING;
    }

    //--------------------------------------------------------------------------------------------//

    private Status getState() {
        return Game.songService.getStatus();
    }

    public SongService getService() {
        return Game.songService;
    }

    public BeatmapInfo getBeatmap() {
        return beatmap;
    }

    //--------------------------------------------------------------------------------------------//

    public void bindMusicObserver(IMusicObserver observer) {
        observers.add(observer);
    }

    //--------------------------------------------------------------------------------------------//

    private void notifyControlRequest(MusicOption option) {
        Log.i("MusicManager", "Option " + option.name() + " was received");
        observers.forEach(observer -> {
            if (isValidObserver(observer)) {
                observer.onMusicControlRequest(option, getState());
            }
        });
    }

    private void notifyStateChange(MusicOption option) {
        observers.forEach(observer -> {
            if (isValidObserver(observer)) {
                observer.onMusicStateChange(option, getState());
            }
        });
    }

    private void notifyMusicChange(BeatmapInfo newBeatmap) {
        observers.forEach(observer -> {
            if (isValidObserver(observer)) {
                observer.onMusicChange(newBeatmap);
            }
        });
    }

    public void onMusicEnd() {
        observers.forEach(observer -> {
            if (isValidObserver(observer)) {
                observer.onMusicEnd();
            }
        });
    }

    private boolean isValidObserver(IMusicObserver observer) {
        if (observer != null) {
            if (observer.getAttachedScreen() != null) {
                return observer.getAttachedScreen() == Game.engine.getCurrentScreen();
            }
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public void change(BeatmapInfo beatmap) {
        if (isInvalidRequest() || beatmap == null)
            return;

        if (getState() != Status.STOPPED) {
            getService().stop();
        }
        int index = Game.library.findBeatmap(beatmap);
        this.beatmap = Game.library.getBeatmapByIndex(index);
        play();
    }

    public void play() {
        if (beatmap == null) {
            beatmap = Game.library.getBeatmap();
        }

        if (isInvalidRequest(Status.PAUSED, Status.STOPPED))
            return;

        notifyControlRequest(PLAY);

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

        notifyControlRequest(PAUSE);
        getService().pause();
        notifyStateChange(PAUSE);
    }

    public void stop() {
        if (isInvalidRequest(Status.PLAYING, Status.PAUSED))
            return;

        notifyControlRequest(STOP);
        getService().stop();
        notifyStateChange(STOP);
    }

    public void previous() {
        if (isInvalidRequest())
            return;

        notifyControlRequest(PREVIOUS);
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
        if (isInvalidRequest())
            return;

        notifyControlRequest(NEXT);
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

        for (int i = 0; i < observers.size(); ++i) {
            observers.get(i).onMusicSync(getState());
        }
    }

    //--------------------------------------------------------------------------------------------//


}
