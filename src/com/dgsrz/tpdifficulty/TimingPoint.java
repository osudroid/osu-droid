package com.dgsrz.tpdifficulty;

/**
 * Created by Fuuko on 2015/5/29.
 */
public class TimingPoint {
    private float bpm;
    private float offset;
    private float speed;

    public TimingPoint(float bpm, float offset, float speed) {
        this.bpm = bpm;
        this.offset = offset;
        this.speed = speed;
    }

    public float getBpm() {
        return bpm;
    }

    public void setBpm(float bpm) {
        this.bpm = bpm;
    }

    public float getOffset() {
        return offset;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public String toString() {
        return "TimingPoint{" +
                "bpm=" + bpm +
                ", offset=" + offset +
                ", speed=" + speed +
                '}';
    }
}
