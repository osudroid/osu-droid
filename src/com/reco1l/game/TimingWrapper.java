package com.reco1l.game;

// Created by Reco1l on 21/9/22 00:26

import android.util.Log;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.ccfit.zuev.osu.BeatmapData;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.OSUParser;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.TimingPoint;

public class TimingWrapper {

    public boolean isContinuousKiai = false;

    public int beat;

    public float
            BeatLength, lastBeatLength,
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
                isContinuousKiai = false;
                if (observer != null) {
                    observer.onKiaiEnd();
                }
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
        BeatLength = firstPoint.getBeatLength() * 1000f;
    }

    //--------------------------------------------------------------------------------------------//

    public void setBeatLength(float length) {
        lastBeatLength = BeatLength;
        BeatLength = length;
    }

    public void restoreBPMLength() {
        BeatLength = lastBeatLength;
    }

    //--------------------------------------------------------------------------------------------//

    public void computeCurrentBpmLength() {
        if (currentPoint != null) {
            BeatLength = currentPoint.getBeatLength() * 1000;
        }
    }

    public boolean computeFirstBpmLength() {
        if (firstPoint != null) {
            BeatLength = firstPoint.getBeatLength() * 1000;
            return true;
        }
        return false;
    }

    public void computeOffset() {
        if (lastPoint != null) {
            offset = lastPoint.getTime() * 1000f % BeatLength;
        }
    }

    public void computeOffsetAtPosition(int position) {
        if (lastPoint != null) {
            offset = (position - lastPoint.getTime() * 1000f) % BeatLength;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public interface Observer {
        default void onKiaiStart() {}
        default void onKiaiEnd() {}
        default void onBeatUpdate(float BPMLength, int beat) {}
    }

    //--------------------------------------------------------------------------------------------//

    public void update(float elapsed, int position) {
        beatPassTime += elapsed * 1000f;

        if (beatPassTime - lastBeatPassTime >= BeatLength - offset) {
            this.lastBeatPassTime = beatPassTime;
            this.offset = 0;

            new Timer().schedule(new TimerTask() {
                public void run() {
                    beat++;
                    cancel(); // Cleaning timer
                }
            }, (long) BeatLength);

            if (beat > 3) {
                beat = 0;
            }

            if (observer != null) {
                observer.onBeatUpdate(BeatLength, beat);
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
            }
        }
    }
}
