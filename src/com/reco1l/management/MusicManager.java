package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.interfaces.MusicObserver;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public final class MusicManager {

    public static final MusicManager instance = new MusicManager();

    private final ArrayList<MusicObserver> observers;

    private TrackInfo track;
    private String mLastPath;

    private float mSpeed = 1f;
    private boolean mShiftPitch = false;

    //--------------------------------------------------------------------------------------------//

    public MusicManager() {
        this.observers = new ArrayList<>();
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
            mLastPath = track.getMusic();
        } else {
            mLastPath = null;
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
                return observer.getAttachedScreen() == Game.engine.getScreen();
            }
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//
    public void setPlayback(float pSpeed, boolean pPitchShift) {
        mSpeed = pSpeed;
        mShiftPitch = pPitchShift;

        int lastPosition = Game.songService.getPosition();

        Game.songService.stop();
        Game.songService.preLoad(mLastPath, pSpeed, pPitchShift);
        Game.songService.play();
        Game.songService.setPosition(lastPosition);
    }

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
        String newPath = track.getMusic();

        if (!Objects.equals(newPath, mLastPath)) {
            Game.songService.stop();
            Game.songService.preLoad(newPath, mSpeed, mShiftPitch);
        }

        Game.libraryManager.findBeatmap(track.getBeatmap());
        onMusicChange(track, Objects.equals(newPath, mLastPath));

        mLastPath = newPath;

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
            Game.songService.preLoad(track.getMusic(), mSpeed, mShiftPitch);
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
