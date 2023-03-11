package com.reco1l.management.music;

// Created by Reco1l on 18/9/22 20:15

import static main.audio.Status.*;

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.management.ExceptionManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import main.audio.Status;
import main.osu.BeatmapInfo;
import main.osu.Config;
import main.osu.TrackInfo;

public final class MusicManager {

    public static final MusicManager instance = new MusicManager();

    private final ArrayList<IMusicObserver> mObservers;

    private TrackInfo mTrack;

    private float mSpeed = 1f;
    private boolean mPitch = false;

    private int
            mTrackIndex = 0,
            mBeatmapIndex = 0;

    //--------------------------------------------------------------------------------------------//

    public MusicManager() {
        this.mObservers = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    private boolean isInvalidRequest(String action, Status... validStates) {
        if (Game.songService == null) {
            Log.e("MusicManager", "SongService is not initialized!");
            return true;
        }
        if (validStates.length > 0) {
            for (Status state : validStates) {
                if (getState() == state) {
                    return false;
                }
            }
            Log.e("MusicManager", "Cannot call " + action.toUpperCase() + " while state is " + getState().name());
            return true;
        }
        return false;
    }

    public boolean isPlaying() {
        return Game.songService != null && Game.songService.getStatus() == PLAYING;
    }

    private Status getState() {
        return Game.songService.getStatus();
    }

    //--------------------------------------------------------------------------------------------//

    public int getTrackIndex() {
        return mTrackIndex;
    }

    public int getBeatmapIndex() {
        return mBeatmapIndex;
    }

    //--------------------------------------------------------------------------------------------//

    public TrackInfo getTrack() {
        return mTrack;
    }

    public BeatmapInfo getBeatmap() {
        if (mTrack == null) {
            return null;
        }
        return mTrack.getBeatmap();
    }

    //--------------------------------------------------------------------------------------------//

    public void bindMusicObserver(IMusicObserver observer) {
        mObservers.add(observer);
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicEnd() {
        notify(IMusicObserver::onMusicEnd);
    }

    private void onMusicChange(TrackInfo track, boolean sameAudio) {
        notify(o -> o.onMusicChange(track, sameAudio));
    }

    private void notify(Consumer<IMusicObserver> consumer) {
        mObservers.forEach(consumer);
    }

    //--------------------------------------------------------------------------------------------//

    public void setPosition(int position) {
        assert Game.songService != null;

        if (position < 0) {
            Game.songService.seekTo(Game.songService.getLength() / 2);
            return;
        }
        Game.songService.seekTo(position);
    }

    public void setVolume(float value) {
        assert Game.songService != null;

        Game.songService.setVolume(value);
    }

    public void setPlayback(float speed, boolean pitch) {
        assert Game.songService != null;

        mSpeed = speed;
        mPitch = pitch;

        int lastPosition = Game.songService.getPosition();

        reset();
        Game.songService.seekTo(lastPosition);
    }

    private boolean sameAudio(TrackInfo track) {
        if (mTrack == null || track == null) {
            return false;
        }

        File last = new File(mTrack.getMusic());
        File New = new File(track.getMusic());

        try {
            return Objects.equals(last.getCanonicalPath(), New.getCanonicalPath());
        }
        catch (IOException e) {
            ExceptionManager.notify(e);
            return false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean change(BeatmapInfo beatmap) {
        if (beatmap != null) {
            return change(beatmap.getPreviewTrack());
        }
        stop();
        onMusicChange(null, false);
        return false;
    }

    public boolean change(TrackInfo track) {
        if (isInvalidRequest("change") || track == null) {
            stop();
            mTrack = null;
            mTrackIndex = 0;
            mBeatmapIndex = 0;

            onMusicChange(null, false);
            return false;
        }
        assert Game.songService != null;

        boolean same = sameAudio(track);

        mTrack = track;
        mTrackIndex = mTrack.getBeatmap().getTracks().indexOf(mTrack);
        mBeatmapIndex = Game.beatmapCollection.indexOf(mTrack.getBeatmap());

        if (!same) {
            Game.songService.stop();
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mPitch);
        }

        Game.libraryManager.findBeatmap(mTrack.getBeatmap());

        if (getState() == STOPPED) {
            Game.songService.play();
            Game.songService.setVolume(Config.getBgmVolume());
        }
        onMusicChange(mTrack, same);
        return true;
    }

    public void reset() {
        if (getState() != STOPPED) {
            stop();
        }
        play();
    }

    public void play() {
        if (isInvalidRequest("play", PAUSED, STOPPED)) {
            return;
        }
        assert Game.songService != null;

        if (mTrack == null) {
            change(Game.libraryManager.getBeatmap());
            return;
        }

        if (getState() == STOPPED) {
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mPitch);
        }
        Game.songService.play();
        Game.songService.setVolume(Config.getBgmVolume());

        notify(IMusicObserver::onMusicPlay);
    }

    public void pause() {
        if (isInvalidRequest("pause", PLAYING)) {
            return;
        }
        assert Game.songService != null;

        Game.songService.pause();
        notify(IMusicObserver::onMusicPause);
    }

    public void stop() {
        if (isInvalidRequest("stop", PLAYING, PAUSED)) {
            return;
        }
        assert Game.songService != null;

        Game.songService.stop();
        notify(IMusicObserver::onMusicStop);
    }

    public void previous() {
        if (isInvalidRequest("previous")) {
            return;
        }
        change(Game.libraryManager.getPrevBeatmap());
    }

    public void next() {
        if (isInvalidRequest("next")) {
            return;
        }
        change(Game.libraryManager.getNextBeatmap());
    }
}
