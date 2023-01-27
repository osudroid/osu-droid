package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.interfaces.MusicObserver;
import com.reco1l.tables.NotificationTable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public final class MusicManager {

    public static final MusicManager instance = new MusicManager();

    private final ArrayList<MusicObserver> mObservers;

    private TrackInfo mTrack;

    private float mSpeed = 1f;
    private boolean mPitch = false;

    private int mTrackIndex = 0;

    //--------------------------------------------------------------------------------------------//

    public MusicManager() {
        this.mObservers = new ArrayList<>();
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

    public int getTrackIndex() {
        return mTrackIndex;
    }

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

    public void bindMusicObserver(MusicObserver observer) {
        mObservers.add(observer);
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicEnd() {
        notify(MusicObserver::onMusicEnd);
    }

    private void onMusicChange(TrackInfo track, boolean sameAudio) {
        notify(o -> o.onMusicChange(track, sameAudio));
    }

    private void notify(Consumer<MusicObserver> consumer) {
        mObservers.forEach(observer -> {
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

        Game.songService.stop();
        Game.songService.preLoad(mTrack.getMusic(), speed, pitch);
        Game.songService.play();
        Game.songService.setPosition(lastPosition);
    }

    private boolean sameAudio(TrackInfo track) {
        if (mTrack == null || track == null) {
            return false;
        }

        File last = new File(mTrack.getMusic());
        File New = new File(track.getMusic());

        try {
            return Objects.equals(last.getCanonicalPath(), New.getCanonicalPath());
        } catch (IOException e) {
            NotificationTable.exception(e);
            return false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean change(BeatmapInfo beatmap) {
        if (beatmap != null) {
            return change(beatmap.getTrack(0));
        }
        stop();
        onMusicChange(null, false);
        return false;
    }

    public boolean change(TrackInfo track) {
        if (isInvalidRequest() || track == null) {
            stop();
            mTrack = null;
            mTrackIndex = 0;

            onMusicChange(null, false);
            return false;
        }
        assert Game.songService != null;

        boolean same = sameAudio(track);
        mTrack = track;
        mTrackIndex = mTrack.getBeatmap().getTracks().indexOf(mTrack);

        if (!same) {
            Game.songService.stop();
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mPitch);
        }

        Game.libraryManager.findBeatmap(mTrack.getBeatmap());

        if (getState() == Status.STOPPED) {
            Game.songService.play();
            Game.songService.setVolume(Config.getBgmVolume());
        }
        onMusicChange(mTrack, same);
        return true;
    }

    public void reset() {
        Game.musicManager.stop();
        Game.musicManager.play();
    }

    public void play() {
        if (isInvalidRequest(Status.PAUSED, Status.STOPPED)) {
            return;
        }
        assert Game.songService != null;

        if (mTrack == null) {
            change(Game.libraryManager.getBeatmap());
            return;
        }

        if (getState() == Status.STOPPED) {
            Game.songService.preLoad(mTrack.getMusic());
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mPitch);
        }
        Game.songService.play();
        Game.songService.setVolume(Config.getBgmVolume());

        notify(MusicObserver::onMusicPlay);
    }

    public void pause() {
        if (isInvalidRequest(Status.PLAYING)) {
            return;
        }
        assert Game.songService != null;

        Game.songService.pause();
        notify(MusicObserver::onMusicPause);
    }

    public void stop() {
        if (isInvalidRequest(Status.PLAYING, Status.PAUSED)) {
            return;
        }
        assert Game.songService != null;

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
