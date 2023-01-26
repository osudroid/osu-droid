package com.reco1l.management;

// Created by Reco1l on 18/9/22 20:15

import android.util.Log;

import androidx.core.math.MathUtils;

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
    private boolean mShiftPitch = false;

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

    public TrackInfo getTrack() {
        return mTrack;
    }

    //--------------------------------------------------------------------------------------------//

    public void bindMusicObserver(MusicObserver observer) {
        mObservers.add(observer);
    }

    //--------------------------------------------------------------------------------------------//

    public void onMusicEnd() {
        notify(MusicObserver::onMusicEnd);
    }

    private void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        notify(o -> o.onMusicChange(track, wasAudioChanged));
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

    public void setVolume(int value) {
        assert Game.songService != null;

        Game.songService.setVolume(MathUtils.clamp(value, 0, 100) / 100f);
    }

    public void setPlayback(float speed, boolean shiftPitch) {
        assert Game.songService != null;

        mSpeed = speed;
        mShiftPitch = shiftPitch;

        int lastPosition = Game.songService.getPosition();

        Game.songService.stop();
        Game.songService.preLoad(mTrack.getMusic(), speed, shiftPitch);
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
        onMusicChange(null, true);
        return false;
    }

    public boolean change(TrackInfo track) {
        if (isInvalidRequest() || track == null) {
            stop();
            onMusicChange(null, true);
            return false;
        }
        assert Game.songService != null;

        boolean same = sameAudio(track);
        mTrack = track;

        if (!same) {
            Game.songService.stop();
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mShiftPitch);
        }

        Game.libraryManager.findBeatmap(mTrack.getBeatmap());
        onMusicChange(mTrack, same);

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
        assert Game.songService != null;

        if (mTrack == null) {
            change(Game.libraryManager.getBeatmap());
            return;
        }

        if (getState() == Status.STOPPED) {
            Game.songService.preLoad(mTrack.getMusic());
            Game.songService.preLoad(mTrack.getMusic(), mSpeed, mShiftPitch);
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
