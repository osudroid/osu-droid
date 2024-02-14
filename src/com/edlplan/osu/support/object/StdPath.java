package com.edlplan.osu.support.object;

import com.edlplan.framework.math.Vec2;

import java.util.ArrayList;
import java.util.List;

public class StdPath {
    private Type type;
    private List<Vec2> controlPoints;

    public StdPath() {
        controlPoints = new ArrayList<>();
    }

    public void addControlPoint(Vec2 p) {
        controlPoints.add(p);
    }

    public void addControlPoint(float x, float y) {
        addControlPoint(new Vec2(x, y));
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Vec2> getControlPoints() {
        return controlPoints;
    }

    public void setControlPoints(List<Vec2> controlPoints) {
        this.controlPoints = controlPoints;
    }

    public enum Type {
        Linear("L"), Perfect("P"), Bezier("B"), Catmull("C");

        public final String tag;

        Type(String t) {
            tag = t;
        }

        public static Type forName(String n) {
            return switch (n) {
                case "L" -> Linear;
                case "P" -> Perfect;
                case "B" -> Bezier;
                case "C" -> Catmull;
                default -> null;
            };
        }

        public String getTag() {
            return tag;
        }
    }
}