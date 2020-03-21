package test.tpdifficulty.hitobject;

import android.graphics.PointF;

import test.tpdifficulty.TimingPoint;

/**
 * Created by Fuuko on 2015/5/29.
 */
public abstract class HitObject {
    protected int startTime;
    protected int endTime;
    protected HitObjectType type;
    protected PointF pos;
    protected TimingPoint timingPoint;
    protected int repeat;

    public HitObject(int startTime, int endTime, HitObjectType type, PointF pos, TimingPoint timingPoint) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.pos = pos;
        this.timingPoint = timingPoint;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public HitObjectType getType() {
        return type;
    }

    public void setType(HitObjectType type) {
        this.type = type;
    }

    public PointF getPos() {
        return pos;
    }

    public void setPos(PointF pos) {
        this.pos = pos;
    }

    public int getLength() {
        return this.endTime - this.startTime;
    }

    public TimingPoint getTimingPoint() {
        return timingPoint;
    }

    public void setTimingPoint(TimingPoint timingPoint) {
        this.timingPoint = timingPoint;
    }

    @Override
    public String toString() {
        return "startTime=" + startTime +
                ", endTime=" + endTime +
                ", pos=" + pos +
                ", timingPoint=" + timingPoint;
    }

    public String toString(float sliderTick, float sliderSpeed) {
        return "";
    }

    public abstract int getCombo(float sliderTick, float sliderSpeed);
}
