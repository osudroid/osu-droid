package com.edlplan.osu.support.slider;

import com.edlplan.andengine.Triangle3DBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.Vec3;
import com.edlplan.framework.math.line.AbstractPath;

public class Draw3DLinePath {
    private static final int MAXRES = 24;
    public float alpha;
    public float width;
    private Triangle3DBuilder triangles;
    private AbstractPath path;

    private float zEdge = -1, zCenter = 1;

    public Draw3DLinePath(AbstractPath p, float width, float zCenter, float zEdge) {
        this.zCenter = zCenter;
        this.zEdge = zEdge;
        alpha = 1;
        path = p;
        this.width = width;
    }

    public void setZCenter(float zCenter) {
        this.zCenter = zCenter;
    }

    public void setZEdge(float zEdge) {
        this.zEdge = zEdge;
    }

    public Triangle3DBuilder getTriangles() {
        if (triangles == null) {
            triangles = new Triangle3DBuilder();
            init();
        }
        return triangles;
    }

    private void addLineCap(Vec2 org, float theta, float thetaDiff) {
        final float step = FMath.Pi / MAXRES;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        //MLog.test.vOnce("dir","gl_test","dir: "+dir);
        int amountPoints = (int) Math.ceil(thetaDiff / step);

        if (dir < 0)
            theta += FMath.Pi;

        /* current = org + atCircle(...)*width */
        Vec3 current = new Vec3(Vec2.atCircle(theta).zoom(width).add(org), zEdge);

        Vec3 orgAtLayer3D = new Vec3(org, zCenter);
        for (int i = 1; i <= amountPoints; i++) {
            triangles.add(
                    orgAtLayer3D,
                    current,
                    current = new Vec3(
                            Vec2.atCircle(theta + dir * Math.min(i * step, thetaDiff))
                                    .zoom(width)
                                    .add(org),
                            zEdge
                    )
            );
        }
    }

    private void addLineQuads(Vec2 ps, Vec2 pe) {
        Vec2 oth_expand = Vec2.lineOthNormal(ps, pe).zoom(width);

        Vec3 startL = new Vec3(ps.copy().add(oth_expand), zEdge);
        Vec3 startR = new Vec3(ps.copy().minus(oth_expand), zEdge);
        Vec3 endL = new Vec3(pe.copy().add(oth_expand), zEdge);
        Vec3 endR = new Vec3(pe.copy().minus(oth_expand), zEdge);
        Vec3 start = new Vec3(ps, zCenter);
        Vec3 end = new Vec3(pe, zCenter);

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
