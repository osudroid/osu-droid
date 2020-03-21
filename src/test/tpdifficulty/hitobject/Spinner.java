package test.tpdifficulty.hitobject;

import android.graphics.PointF;

import test.tpdifficulty.TimingPoint;

/**
 * Created by Fuuko on 2015/5/29.
 */
public class Spinner extends HitObject {

    public Spinner(int startTime, int endTime, PointF pos, TimingPoint timingPoint) {
        super(startTime, endTime, HitObjectType.Spinner, pos, timingPoint);
        this.repeat = 1;
    }

    @Override
    public String toString() {
        return "Spinner{" +
                super.toString() +
                ", Combo=" + getCombo(0, 0) +
                "}";
    }

    @Override
    public int getCombo(float sliderTick, float sliderSpeed) {
        return 1;
    }
}
