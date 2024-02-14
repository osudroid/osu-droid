package com.edlplan.osu.support.object;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.edlplan.framework.math.line.approximator.BezierApproximator;
import com.edlplan.framework.math.line.approximator.CatmullApproximator;
import com.edlplan.framework.math.line.approximator.CircleApproximator;

import java.util.ArrayList;
import java.util.List;

public class StdSliderPathMaker {

    private StdPath slider;

    private LinePath path;

    public StdSliderPathMaker(StdPath sld) {
        slider = sld;
        path = new LinePath();
    }

    public List<Vec2> getControlPoint() {
        return slider.getControlPoints();
    }

    public List<Vec2> calculateSubPath(List<Vec2> subPoints) {
        switch (slider.getType()) {
            case Linear -> {
                return subPoints;
            }
            case Perfect -> {
                if (getControlPoint().size() == 3 && subPoints.size() == 3) {
                    List<Vec2> sub = (new CircleApproximator(subPoints.get(0), subPoints.get(1), subPoints.get(2))).createArc();
                    if (sub.size() != 0) return sub;
                }
            }
            case Catmull -> {
                return (new CatmullApproximator(subPoints)).createCatmull();
            }
        }
        return (new BezierApproximator(subPoints)).createBezier();
    }

    public LinePath calculatePath() {
        path.clear();
        List<Vec2> subControlPoints = new ArrayList<>();
        for (int i = 0; i < getControlPoint().size(); i++) {
            subControlPoints.add(getControlPoint().get(i));
            if (i == getControlPoint().size() - 1 || getControlPoint().get(i).equals(getControlPoint().get(i + 1))) {
                List<Vec2> subPath = calculateSubPath(subControlPoints);
                for (Vec2 v : subPath) {
                    if (path.size() == 0 || !path.getLast().equals(v)) {
                        path.add(v);
                    }
                }
                subControlPoints.clear();
            }
        }
        return path;
    }
}
