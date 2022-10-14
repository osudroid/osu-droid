package com.reco1l.game;

// Created by Reco1l on 21/9/22 00:26

import android.util.Log;

import java.util.LinkedList;

import ru.nsu.ccfit.zuev.osu.BeatmapData;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.OSUParser;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;

public class TimingWrapper {

    public boolean isContinuousKiai = false;

    public float
            BPMLength, lastBPMLength,
            beatPassTime, lastBeatPassTime,
            offset;

    public TimingPoint
            currentPoint,
            lastPoint,
            firstPoint;

    private LinkedList<TimingPoint> timingPoints;

    private Observer observer;

    //--------------------------------------------------------------------------------------------//

    public TimingWrapper() {
        this.timingPoints = new LinkedList<>();
    }

    //--------------------------------------------------------------------------------------------//

    public void loadPointsFrom(BeatmapInfo beatmap) {
        TrackInfo track = beatmap.getTrack(0);

        if (track != null) {
            OSUParser parser = new OSUParser(track.getFilename());

            if (parser.openFile()) {
                Log.i("TimingWrapper", "Parsed points from: " + track.getPublicName());
                timingPoints = new LinkedList<>();
                currentPoint = null;
                parsePoints(parser.readData());
            }
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
        BPMLength = firstPoint.getBeatLength() * 1000f;
    }

    //--------------------------------------------------------------------------------------------//

    public void setBPMLength(float length) {
        lastBPMLength = BPMLength;
        BPMLength = length;
    }

    public void restoreBPMLength() {
        BPMLength = lastBPMLength;
    }

    //--------------------------------------------------------------------------------------------//

    public void computeCurrentBpmLength() {
        if (currentPoint != null) {
            BPMLength = currentPoint.getBeatLength() * 1000;
            Log.i("TimingWrapper", "Current BPM length: " + BPMLength);
        }
    }

    public boolean computeFirstBpmLength() {
        if (firstPoint != null) {
            BPMLength = firstPoint.getBeatLength() * 1000;
            Log.i("TimingWrapper", "Current BPM length: " + BPMLength);
            return true;
        }
        return false;
    }

    public void computeOffset() {
        if (lastPoint != null) {
            offset = lastPoint.getTime() * 1000f % BPMLength;
            Log.i("TimingWrapper", "Current offset: " + offset);
        }
    }

    public void computeOffsetAtPosition(int position) {
        if (lastPoint != null) {
            offset = (position - lastPoint.getTime() * 1000f) % BPMLength;
            Log.i("TimingWrapper", "Current offset: " + offset);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public interface Observer {
        default void onKiaiStart() {}
        default void onKiaiEnd() {}
        default void onBpmUpdate(float BPMLength) {}
    }

    //--------------------------------------------------------------------------------------------//

    public void update(float elapsed, int position) {
        beatPassTime += elapsed * 1000f;

        if (beatPassTime - lastBeatPassTime >= BPMLength - offset) {
            this.lastBeatPassTime = beatPassTime;
            this.offset = 0;

            if (observer != null) {
                observer.onBpmUpdate(BPMLength);
                Log.i("TimingWrapper", "BPM length: " + BPMLength);
            }
        }

        if (position < 0)
            return;

        if (currentPoint != null && position > currentPoint.getTime() * 1000) {
            if (observer != null) {
                if (!isContinuousKiai && currentPoint.isKiai()) {
                    observer.onKiaiStart();
                } else if (isContinuousKiai && !currentPoint.isKiai()) {
                    observer.onKiaiEnd();
                }
            }
            isContinuousKiai = currentPoint.isKiai();

            if (timingPoints.size() > 0) {
                currentPoint = timingPoints.remove(0);
                if (!currentPoint.wasInderited()) {
                    lastPoint = currentPoint;
                    computeCurrentBpmLength();
                    computeOffset();
                }
            } else {
                currentPoint = null;
                Log.i("TimingWrapper", "No timing points found!");
            }
        } else if (currentPoint == null) {
            Log.i("TimingWrapper", "Current timing point is null!");
        }
    }
}
