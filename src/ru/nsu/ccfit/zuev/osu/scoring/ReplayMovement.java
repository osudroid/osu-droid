package ru.nsu.ccfit.zuev.osu.scoring;

import android.graphics.PointF;

public class ReplayMovement {
    protected int time;
    protected PointF point = new PointF();
    protected TouchType touchType;

    public int getTime() {
        return time;
    }
    public PointF getPoint() { return point; }
    public TouchType getTouchType() {
        return touchType;
    }
}
