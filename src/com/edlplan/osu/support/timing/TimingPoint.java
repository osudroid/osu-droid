package com.edlplan.osu.support.timing;

import com.edlplan.framework.utils.U;
import com.edlplan.osu.support.SampleSet;
import com.edlplan.osu.support.timing.controlpoint.ControlPoint;

public class TimingPoint extends ControlPoint {

    /**
     * 在lazer原代码里这里是一个enum TimeSignatures,
     * 定义了一拍里有几小节（大概是这么叫的。。。滚去学乐理了）
     */
    private int meter;

    /**
     * 定义音效类型，赞时放置，之后应该用enum代替
     */
    private int sampleType;

    private SampleSet sampleSet = SampleSet.None;

    private int volume = 100;

    /**
     * 在lazer里叫timingChange，一般mapper以Editor里的线的颜色区分
     */
    private boolean inherited = true;

    private boolean kiaiMode = false;

    private boolean omitFirstBarSignature = false;

    private double beatLength;

    private double speedMultiplier;

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getBeatLength() {
        return beatLength;
    }

    public void setBeatLength(double beatLength) {
        this.beatLength = beatLength;
        speedMultiplier = (beatLength < 0 ? (100.0 / -beatLength) : 1);
    }

    public boolean isOmitFirstBarSignature() {
        return omitFirstBarSignature;
    }

    public void setOmitFirstBarSignature(boolean omitFirstBarSignature) {
        this.omitFirstBarSignature = omitFirstBarSignature;
    }

    public SampleSet getSampleSet() {
        return sampleSet;
    }

    public void setSampleSet(SampleSet sampleSet) {
        this.sampleSet = sampleSet;
    }

    public int getMeter() {
        return meter;
    }

    public void setMeter(int meter) {
        this.meter = meter;
    }

    public int getSampleType() {
        return sampleType;
    }

    public void setSampleType(int sampleType) {
        this.sampleType = sampleType;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public boolean isKiaiMode() {
        return kiaiMode;
    }

    public void setKiaiMode(boolean kiaiMode) {
        this.kiaiMode = kiaiMode;
    }

    @Override
    public String toString() {

        String sb = getTime() + "," +
                getBeatLength() + "," +
                getMeter() + "," +
                getSampleType() + "," +
                getSampleSet() + "," +
                U.toVString(isInherited()) + "," +
                (((isKiaiMode()) ? 1 : 0) + ((isOmitFirstBarSignature()) ? 8 : 0));
        return sb;
    }
}
