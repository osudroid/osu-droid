package com.edlplan.osu.support.timing;

import com.edlplan.framework.utils.U;
import com.edlplan.osu.support.SampleSet;

import java.util.ArrayList;
import java.util.List;

public class TimingPoints {
    private ArrayList<TimingPoint> timings;

    public TimingPoints() {
        timings = new ArrayList<TimingPoint>();
    }

    public static TimingPoints parse(List<String> strings) {
        TimingPoints timingPoints = new TimingPoints();
        for (String ll : strings) {
            String[] l = ll.split(",");
            TimingPoint t = new TimingPoint();
            t.setTime((int) Math.round(Double.parseDouble(l[0])));
            t.setBeatLength(Double.parseDouble(l[1]));
            t.setMeter(l.length > 2 ? Integer.parseInt(l[2]) : 4);
            t.setSampleType(l.length > 3 ? Integer.parseInt(l[3]) : 1);
            t.setSampleSet(l.length > 4 ? SampleSet.parse(l[4]) : SampleSet.None);
            t.setVolume(l.length > 5 ? Integer.parseInt(l[5]) : 100);
            t.setInherited(t.getBeatLength() < 0);
            int eff = l.length > 7 ? Integer.parseInt(l[7]) : 0;
            t.setKiaiMode((eff & 1) > 0);
            t.setOmitFirstBarSignature((eff & 8) > 0);
            timingPoints.addTimingPoint(t);
        }
        return timingPoints;
    }

    public void addTimingPoint(TimingPoint t) {
        timings.add(t);
    }

    public ArrayList<TimingPoint> getTimingPointList() {
        return timings;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (TimingPoint t : timings) {
            sb.append(t.toString()).append(U.NEXT_LINE);
        }
        return sb.toString();
    }
}
