package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;

public class DrawLinePath {
    private static final int MAXRES = 24;

    private static final float Z_MIDDLE = 99.0f;

    private static final float Z_SIDE = -99.0f;
    public float alpha;
    public float width;
    Vec2 current, current2;
    Vec2 startL = new Vec2();
    Vec2 startR = new Vec2();
    Vec2 endL = new Vec2();
    Vec2 endR = new Vec2();
    Vec2 start = new Vec2();
    Vec2 end = new Vec2();
    Vec2 oth_expand = new Vec2();
    private TriangleBuilder triangles;
    private AbstractPath path;

    public DrawLinePath(AbstractPath p, float width) {
        alpha = 1;
        path = p;
        this.width = width;
    }

    public DrawLinePath() {
        alpha = 1;
        this.width = width;
    }

    public DrawLinePath reset(AbstractPath p, float width) {
        alpha = 1;
        path = p;
        this.width = width;
        if (triangles != null) {
            triangles.length = 0;
        }
        return this;
    }

    public TriangleBuilder getTriangles() {
        if (triangles == null) {
            triangles = new TriangleBuilder(path.size() * 6);
            init();
        }
        return triangles;
    }

    public TriangleBuilder getTriangles(TriangleBuilder builder) {
        TriangleBuilder cache = triangles;
        if (cache != null) {
            cache.getVertex(builder);
        } else {
            triangles = builder;
            builder.length = 0;
            init();
        }
        triangles = cache;
        return builder;
    }


    private void addLineCap(Vec2 org, float theta, float thetaDiff) {
        final float step = FMath.Pi / MAXRES;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        int amountPoints = (int) Math.ceil(thetaDiff / step);

        if (dir < 0)
            theta += FMath.Pi;

        /* current = org + atCircle(...)*width */
        current = Vec2.atCircle(theta, current).zoom(width).add(org);

        for (int i = 1; i <= amountPoints; i++) {
            current2 = Vec2.atCircle(theta + dir * Math.min(i * step, thetaDiff), current2)
                    .zoom(width)
                    .add(org);
            triangles.add(
                    org,
                    current,
                    current2
            );
            current.set(current2);
        }
    }

    private void addLineQuads(Vec2 ps, Vec2 pe) {
        oth_expand = Vec2.lineOthNormal(ps, pe, oth_expand).zoom(width);

        startL.set(ps);
        startL.add(oth_expand);
        startR.set(ps);
        startR.minus(oth_expand);
        endL.set(pe);
        endL.add(oth_expand);
        endR.set(pe);
        endR.minus(oth_expand);
        start = ps;
        end = pe;

        triangles.add(
                start,
                end,
                endL
        );

        triangles.add(
                start,
                endL,
                startL
        );

        triangles.add(
                start,
                endR,
                end
        );

        triangles.add(
                start,
                startR,
                endR
        );
    }

    private void init() {
        if (path.size() < 2) {
            if (path.size() == 1) {
                addLineCap(path.get(0), FMath.Pi, FMath.Pi);
                addLineCap(path.get(0), 0, FMath.Pi);
                return;
            } else {
                return;
                //throw new RuntimeException("Path must has at least 1 point");
            }
        }

        float theta = Vec2.calTheta(path.get(0), path.get(1));
        addLineCap(path.get(0), theta + FMath.PiHalf, FMath.Pi);
        addLineQuads(path.get(0), path.get(1));
        if (path.size() == 2) {
            addLineCap(path.get(1), theta - FMath.PiHalf, FMath.Pi);
            return;
        }
        Vec2 nowPoint = path.get(1);
        Vec2 nextPoint;
        float preTheta = theta;
        float nextTheta;
        int max_i = path.size();
        for (int i = 2; i < max_i; i++) {
            nextPoint = path.get(i);
            nextTheta = Vec2.calTheta(nowPoint, nextPoint);
            addLineCap(nowPoint, preTheta - FMath.PiHalf, nextTheta - preTheta);
            addLineQuads(nowPoint, nextPoint);
            nowPoint = nextPoint;
            preTheta = nextTheta;
        }
        addLineCap(path.get(max_i - 1), preTheta - FMath.PiHalf, FMath.Pi);
    }


}
