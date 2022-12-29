package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.interfaces.MusicObserver;

import java.util.ArrayList;
import java.util.function.Consumer;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public final class MusicManager {

    private static MusicManager instance;

    private final ArrayList<MusicObserver> observers;

    private TrackInfo track;
    private String musicPath;

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
        if (Game.songService == null) {
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

    public TrackInfo getTrack() {
        return track;
    }

    //--------------------------------------------------------------------------------------------//

    public void bindMusicObserver(MusicObserver observer) {
        observers.add(observer);
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicEnd() {
        notify(MusicObserver::onMusicEnd);
    }

    private void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        notify(o -> o.onMusicChange(track, wasAudioChanged));

        if (track != null) {
            musicPath = track.getMusic();
        } else {
            musicPath = null;
        }
    }

    private void notify(Consumer<MusicObserver> consumer) {
        observers.forEach(observer -> {
            if (isValidObserver(observer)) {
                consumer.accept(observer);
            }
        });
    }

    private boolean isValidObserver(MusicObserver observer) {
        if (observer != null) {
            if (observer.getAttachedScreen() != null) {
                return observer.getAttachedScreen() == Game.engine.getCurrentScreen();
            }
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean change(BeatmapInfo beatmap) {
        if (beatmap != null) {
            return change(beatmap.getTrack(0));
        }
        stop();
        onMusicChange(null, true);
        return false;
    }

    public boolean change(TrackInfo newTrack) {
        if (isInvalidRequest() || newTrack == null) {
            stop();
            onMusicChange(null, true);
            return false;
        }
        if (newTrack.equals(track)) {
            return false;
        }
        track = newTrack;
        String newMusic = track.getMusic();

        if (!newMusic.equals(musicPath)) {
            Game.songService.stop();
            Game.songService.preLoad(newMusic);
        }

        Game.libraryManager.findBeatmap(track.getBeatmap());
        onMusicChange(track, newMusic.equals(musicPath));

        if (getState() == Status.STOPPED) {
            Game.songService.play();
            Game.songService.setVolume(Config.getBgmVolume());
        }
        return true;
    }

    public void play() {
        if (isInvalidRequest(Status.PAUSED, Status.STOPPED)) {
            return;
        }
        if (track == null) {
            change(Game.libraryManager.getBeatmap());
            return;
        }

        if (getState() == Status.STOPPED) {
            Game.songService.preLoad(track.getMusic());
        }
        Game.songService.play();
        Game.songService.setVolume(Config.getBgmVolume());

        notify(MusicObserver::onMusicPlay);
    }

    public void pause() {
        if (isInvalidRequest(Status.PLAYING)) {
            return;
        }
        Game.songService.pause();
        notify(MusicObserver::onMusicPause);
    }

    public void stop() {
        if (isInvalidRequest(Status.PLAYING, Status.PAUSED)) {
            return;
        }
        Game.songService.stop();
        notify(MusicObserver::onMusicStop);
    }

    public void previous() {
        if (isInvalidRequest()) {
            return;
        }
        TrackInfo prev = Game.libraryManager.getPrevBeatmap().getTrack(0);
        change(prev);
    }

    public void next() {
        if (isInvalidRequest()) {
            return;
        }
        TrackInfo next = Game.libraryManager.getNextBeatmap().getTrack(0);
        change(next);
    }
}
