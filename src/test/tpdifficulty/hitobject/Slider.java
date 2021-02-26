package test.tpdifficulty.hitobject;

import android.graphics.PointF;

import java.util.ArrayList;

import test.tpdifficulty.TimingPoint;

/**
 * Created by Fuuko on 2015/5/29.
 */
public class Slider extends HitObject {
    private SliderType sliderType;
    private ArrayList<PointF> curvePoints;
    private float rawLength;

    public Slider(int startTime, int endTime, PointF pos, TimingPoint timingPoint, SliderType sliderType,
                  int repeat, ArrayList<PointF> curvePoints, float rawLength) {
        super(startTime, endTime, HitObjectType.Slider, pos, timingPoint);
        this.sliderType = sliderType;
        this.repeat = repeat;
        this.curvePoints = curvePoints;
        this.rawLength = rawLength;
    }

    public SliderType getSliderType() {
        return sliderType;
    }

    public void setSliderType(SliderType sliderType) {
        this.sliderType = sliderType;
    }

    @Override
    public int getLength() {
        return super.getLength() * repeat;
    }

    public float getRawLength() {
        return rawLength;
    }

    public void setRawLength(float rawLength) {
        this.rawLength = rawLength;
    }

    public ArrayList<PointF> getCurvePoints() {
        return curvePoints;
    }

    public void setCurvePoints(ArrayList<PointF> curvePoints) {
        this.curvePoints = curvePoints;
    }

    public String toString() {
        return "Slider{" +
                super.toString() +
                "sliderType=" + sliderType +
                ", repeat=" + repeat +
                ", curvePoints=" + curvePoints +
                '}';
    }

    public String toString(float sliderTick, float sliderSpeed) {
        return "Slider{" +
                super.toString() +
                "sliderType=" + sliderType +
                ", repeat=" + repeat +
                ", curvePoints=" + curvePoints +
                ", Combo=" + getCombo(sliderTick, sliderSpeed) +
                '}';
    }

    @Override
    public int getCombo(float sliderTick, float sliderSpeed) {
        return (int) Math.ceil(sliderTick * rawLength / (timingPoint.getSpeed() * sliderSpeed) / 100.01f) * repeat + 1;
    }
}
