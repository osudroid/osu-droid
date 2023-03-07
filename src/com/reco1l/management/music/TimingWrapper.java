package com.reco1l.management.music;

// Created by Reco1l on 21/9/22 00:26

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.framework.execution.Async;

import java.util.LinkedList;

import main.osu.BeatmapData;
import main.osu.OSUParser;
import main.osu.TrackInfo;
import main.osu.game.TimingPoint;

public class TimingWrapper implements IMusicObserver {

    public static final TimingWrapper instance = new TimingWrapper();

    private TimingPoint
            mLastPoint,
            mFirstPoint,
            mCurrentPoint;

    private LinkedList<TimingPoint> mPoints;

    private short mBeat;

    private float
            mOffset,
            mBeatLength,
            mElapsedTime,
            mLastBeatLength,
            mLastElapsedTime;

    private boolean
            mIsNextBeat = false,
            mIsKiaiSection = false;

    //--------------------------------------------------------------------------------------------//

    public TimingWrapper() {
        mBeatLength = 1000;
        mPoints = new LinkedList<>();
        Game.musicManager.bindMusicObserver(this);
    }

    //--------------------------------------------------------------------------------------------//

    private void clear() {
        mPoints = new LinkedList<>();
        mCurrentPoint = null;
        mIsKiaiSection = false;
    }

    private void loadPointsFrom(TrackInfo track) {
        OSUParser parser = new OSUParser(track.getFilename());

        if (parser.openFile()) {
            Log.i("TimingWrapper", "Parsed points from: " + track.getPublicName());
            parsePoints(parser.readData());
        }
    }

    public void parsePoints(BeatmapData data) {
        if (data == null || data.getData("TimingPoints") == null) {
            Log.i("TimingWrapper", "Data is null!");
            return;
        }

        for (String string : data.getData("TimingPoints")) {
            TimingPoint point = new TimingPoint(string.split("[,]"), mCurrentPoint);
            mPoints.add(point);

            if (!point.wasInderited() || mCurrentPoint == null) {
                mCurrentPoint = point;
            }
        }
        Log.i("TimingWrapper", "Timing points found: " + mPoints.size());
        mFirstPoint = mPoints.removeFirst();
        mCurrentPoint = mFirstPoint;
        mLastPoint = mCurrentPoint;
        mBeatLength = mFirstPoint.getBeatLength() * 1000f;
    }

    //--------------------------------------------------------------------------------------------//

    public void setBeatLength(float length) {
        mLastBeatLength = mBeatLength;
        mBeatLength = length;
    }

    public void restoreBPMLength() {
        mBeatLength = mLastBeatLength;
    }

    //--------------------------------------------------------------------------------------------//

    private void computeCurrentBpmLength() {
        if (mCurrentPoint != null) {
            mBeatLength = mCurrentPoint.getBeatLength() * 1000f;
        }
    }

    private boolean computeFirstBpmLength() {
        if (mFirstPoint != null) {
            mBeatLength = mFirstPoint.getBeatLength() * 1000f;
            return true;
        }
        return false;
    }

    private void computeOffset() {
        if (mLastPoint != null) {
            mOffset = mLastPoint.getTime() * 1000f % mBeatLength;
        }
    }

    private void computeOffsetAtPosition(int position) {
        if (mLastPoint != null) {
            mOffset = (position - mLastPoint.getTime() * 1000f) % mBeatLength;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isKiai() {
        return mIsKiaiSection;
    }

    public boolean isNextBeat() {
        return mIsNextBeat;
    }

    public short getBeat() {
        return mBeat;
    }

    public float getBeatLength() {
        return mBeatLength;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
        clear();

        if (newTrack != null) {
            loadPointsFrom(newTrack);

            if (computeFirstBpmLength()) {
                computeOffset();
            }
        }
    }

    @Override
    public void onMusicPlay() {
        restoreBPMLength();
        computeOffsetAtPosition(Game.songService.getPosition());
    }

    @Override
    public void onMusicPause() {
        setBeatLength(1000);
    }

    @Override
    public void onMusicStop() {
        setBeatLength(1000);
    }

    public void sync() {
        if (Game.musicManager.isPlaying()) {
            computeOffsetAtPosition(Game.songService.getPosition());
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onUpdate(float elapsed) {
        if (Game.musicManager.isPlaying()) {
            update(elapsed, Game.songService.getPosition());
            return;
        }
        update(elapsed, -1);
    }

    private void update(float elapsed, int position) {
        mElapsedTime += elapsed * 1000f;

        if (mElapsedTime - mLastElapsedTime >= mBeatLength - mOffset) {
            mLastElapsedTime = mElapsedTime;
            mOffset = 0;

            mBeat++;
            if (mBeat > 3) {
                mBeat = 0;
            }

            mIsNextBeat = true;
        } else {
            mIsNextBeat = false;
        }

        if (mCurrentPoint != null && position > mCurrentPoint.getTime() * 1000) {
            mIsKiaiSection = mCurrentPoint.isKiai();

            if (mPoints.size() == 0) {
                mCurrentPoint = null;
                return;
            }
            mCurrentPoint = mPoints.remove(0);

            if (!mCurrentPoint.wasInderited()) {
                mLastPoint = mCurrentPoint;
                computeCurrentBpmLength();
                computeOffset();
            }
        }
    }
}
