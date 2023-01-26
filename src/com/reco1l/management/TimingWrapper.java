package com.reco1l.management;

// Created by Reco1l on 21/9/22 00:26

import android.util.Log;

import com.reco1l.Game;
import com.reco1l.interfaces.MusicObserver;
import com.reco1l.utils.execution.ScheduledTask;

import java.util.LinkedList;

import ru.nsu.ccfit.zuev.osu.BeatmapData;
import ru.nsu.ccfit.zuev.osu.OSUParser;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;

public class TimingWrapper implements MusicObserver {

    public static final TimingWrapper instance = new TimingWrapper();

    public TimingPoint
            currentPoint,
            lastPoint,
            firstPoint;

    private LinkedList<TimingPoint> timingPoints;

    private short beat;

    private float
            beatLength, lastBeatLength,
            beatPassTime, lastBeatPassTime,
            offset;

    private boolean
            isNextBeat = false,
            isKiaiSection = false;

    //--------------------------------------------------------------------------------------------//

    public TimingWrapper() {
        beatLength = 1000;
        this.timingPoints = new LinkedList<>();
        Game.musicManager.bindMusicObserver(this);
    }

    //--------------------------------------------------------------------------------------------//

    private void clear() {
        timingPoints = new LinkedList<>();
        currentPoint = null;
        isKiaiSection = false;
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
            TimingPoint point = new TimingPoint(string.split("[,]"), currentPoint);
            timingPoints.add(point);

            if (!point.wasInderited() || currentPoint == null) {
                currentPoint = point;
            }
        }
        Log.i("TimingWrapper", "Timing points found: " + timingPoints.size());
        firstPoint = timingPoints.removeFirst();
        currentPoint = firstPoint;
        lastPoint = currentPoint;
        beatLength = firstPoint.getBeatLength() * 1000f;
    }

    //--------------------------------------------------------------------------------------------//

    public void setBeatLength(float length) {
        lastBeatLength = beatLength;
        beatLength = length;
    }

    public void restoreBPMLength() {
        beatLength = lastBeatLength;
    }

    //--------------------------------------------------------------------------------------------//

    private void computeCurrentBpmLength() {
        if (currentPoint != null) {
            beatLength = currentPoint.getBeatLength() * 1000;
        }
    }

    private boolean computeFirstBpmLength() {
        if (firstPoint != null) {
            beatLength = firstPoint.getBeatLength() * 1000;
            return true;
        }
        return false;
    }

    private void computeOffset() {
        if (lastPoint != null) {
            offset = lastPoint.getTime() * 1000f % beatLength;
        }
    }

    private void computeOffsetAtPosition(int position) {
        if (lastPoint != null) {
            offset = (position - lastPoint.getTime() * 1000f) % beatLength;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isKiai() {
        return isKiaiSection;
    }

    public boolean isNextBeat() {
        return isNextBeat;
    }

    public short getBeat() {
        return beat;
    }

    public float getBeatLength() {
        return beatLength;
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
        beatPassTime += elapsed * 1000f;

        if (beatPassTime - lastBeatPassTime >= beatLength - offset) {
            lastBeatPassTime = beatPassTime;
            offset = 0;

            ScheduledTask.run(() -> {
                beat++;

                if (beat > 3) {
                    beat = 0;
                }
            }, Math.max(1, (long) beatLength));
            isNextBeat = true;
        } else {
            isNextBeat = false;
        }

        if (currentPoint != null && position > currentPoint.getTime() * 1000) {
            isKiaiSection = currentPoint.isKiai();

            if (timingPoints.size() == 0) {
                currentPoint = null;
                return;
            }
            currentPoint = timingPoints.remove(0);

            if (!currentPoint.wasInderited()) {
                lastPoint = currentPoint;
                computeCurrentBpmLength();
                computeOffset();
            }
        }
    }
}
