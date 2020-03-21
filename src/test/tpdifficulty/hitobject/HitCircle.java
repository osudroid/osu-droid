package test.tpdifficulty.hitobject;

import android.graphics.PointF;

import test.tpdifficulty.TimingPoint;

/**
 * Created by Fuuko on 2015/5/29.
 */
public class HitCircle extends HitObject {

    public HitCircle(int startTime, PointF pos, TimingPoint timingPoint) {
        super(startTime, startTime, HitObjectType.Normal, pos, timingPoint);
        this.repeat = 1;
    }

    public String toString() {
        return "HitCircle{" +
                super.toString() +
                ", Combo=" + getCombo(0, 0) +
                "}";
    }

    @Override
    public int getCombo(float sliderTick, float sliderSpeed) {
        return 1;
    }
}
