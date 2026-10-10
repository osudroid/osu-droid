package com.edlplan.osu.support.slider;

import com.edlplan.andengine.TriangleBuilder;
import com.edlplan.framework.math.FMath;
import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.AbstractPath;

public class DrawLinePath {
    private static final int MAXRES = 24;

    public float alpha;
    public float width;
    Vec2 current = new Vec2();
    Vec2 current2 = new Vec2();
    Vec2 startL = new Vec2();
    Vec2 startR = new Vec2();
    Vec2 endL = new Vec2();
    Vec2 endR = new Vec2();
    Vec2 previousEndL = new Vec2();
    Vec2 previousEndR = new Vec2();
    Vec2 start = new Vec2();
    Vec2 end = new Vec2();
    Vec2 oth_expand = new Vec2();
    private TriangleBuilder triangles;
    private AbstractPath path;

    private float[] segmentThetas = new float[0];
    private int[] segmentQuadStartOffsets = new int[0];
    private int segmentQuadStartCount;
    private AbstractPath cachedThetaPath;
    private int cachedThetaPathSize;
    private float cachedFirstX;
    private float cachedFirstY;
    private float cachedLastX;
    private float cachedLastY;

    public DrawLinePath() {
        alpha = 1;
    }

    public DrawLinePath reset(AbstractPath p, float width) {
        alpha = 1;
        path = p;
        this.width = width;
        ensureThetaCache();
        if (triangles != null) {
            triangles.length = 0;
        }
        return this;
    }

    public TriangleBuilder computeTriangles(TriangleBuilder builder) {
        TriangleBuilder cache = triangles;
        if (cache != null) {
            cache.applyVertices(builder);
        } else {
            triangles = builder;
            builder.reset();
            init();
        }
        triangles = cache;
        return builder;
    }

    public int getSegmentQuadStartCount() {
        return segmentQuadStartCount;
    }

    public int getSegmentQuadStartOffset(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= segmentQuadStartCount) {
            return 0;
        }

        return segmentQuadStartOffsets[segmentIndex];
    }


    private void addLineCap(Vec2 org, float theta, float thetaDiff) {
        Vec2 first = Vec2.atCircle(theta, new Vec2()).zoom(width).add(org);
        Vec2 last = Vec2.atCircle(theta + thetaDiff, new Vec2()).zoom(width).add(org);

        addLineCap(org, theta, thetaDiff, first, last);
    }

    /**
     * Adds a cap from {@code first} to {@code last}, the corners of the adjacent quads. They are passed in instead of
     * computed again from the angles, since recomputed corners differ in the last float bits. That leaves hairline
     * gaps between the cap and the quads, which the slider border shows through as single pixels.
     */
    private void addLineCap(Vec2 org, float theta, float thetaDiff, Vec2 first, Vec2 last) {
        final float step = FMath.Pi / MAXRES;

        float dir = Math.signum(thetaDiff);
        thetaDiff *= dir;
        int amountPoints = Math.max(1, (int) Math.ceil(thetaDiff / step));

        if (dir < 0)
            theta += FMath.Pi;

        current.set(first);

        for (int i = 1; i <= amountPoints; i++) {
            if (i == amountPoints) {
                current2.set(last);
            } else {
                current2 = Vec2.atCircle(theta + dir * i * step, current2)
                        .zoom(width)
                        .add(org);
            }

            triangles.add(
                    org,
                    current,
                    current2
            );
            current.set(current2);
        }
    }

    private void computeQuadCorners(Vec2 ps, Vec2 pe) {
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
    }

    /**
     * Adds the quads of the segment whose corners were last computed by {@link #computeQuadCorners}.
     */
    private void addLineQuads() {
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

    private void ensureThetaCache() {
        int size = path == null ? 0 : path.size();

        if (size < 2) {
            cachedThetaPath = path;
            cachedThetaPathSize = size;
            return;
        }

        Vec2 first = path.get(0);
        Vec2 last = path.get(size - 1);

        if (cachedThetaPath == path &&
            cachedThetaPathSize == size &&
            cachedFirstX == first.x && cachedFirstY == first.y &&
            cachedLastX == last.x && cachedLastY == last.y) {
            return;
        }

        int segmentCount = size - 1;

        if (segmentThetas.length < segmentCount) {
            segmentThetas = new float[segmentCount];
        }

        for (int i = 0; i < segmentCount; i++) {
            segmentThetas[i] = Vec2.calTheta(path.get(i), path.get(i + 1));
        }

        cachedThetaPath = path;
        cachedThetaPathSize = size;
        cachedFirstX = first.x;
        cachedFirstY = first.y;
        cachedLastX = last.x;
        cachedLastY = last.y;
    }

    private void init() {
        if (path.size() < 2) {
            segmentQuadStartCount = 0;
            if (path.size() == 1) {
                addLineCap(path.get(0), FMath.Pi, FMath.Pi);
                addLineCap(path.get(0), 0, FMath.Pi);
            }
            return;
        }

        int segmentCount = path.size() - 1;

        if (segmentQuadStartOffsets.length < segmentCount) {
            segmentQuadStartOffsets = new int[segmentCount];
        }

        segmentQuadStartCount = segmentCount;

        float theta = segmentThetas[0];
        computeQuadCorners(path.get(0), path.get(1));
        addLineCap(path.get(0), theta + FMath.PiHalf, FMath.Pi, startL, startR);
        segmentQuadStartOffsets[0] = triangles.length;
        addLineQuads();
        if (path.size() == 2) {
            addLineCap(path.get(1), theta - FMath.PiHalf, FMath.Pi, endR, endL);
            return;
        }
        Vec2 nowPoint = path.get(1);
        Vec2 nextPoint;
        float preTheta = theta;
        float nextTheta;
        int max_i = path.size();
        for (int i = 2; i < max_i; i++) {
            nextPoint = path.get(i);
            nextTheta = segmentThetas[i - 1];

            previousEndL.set(endL);
            previousEndR.set(endR);
            computeQuadCorners(nowPoint, nextPoint);

            // The cap closes the outer side of the turn. The inner side is closed as well, since the quads only
            // overlap there as long as the float corners happen to round that way.
            float thetaDiff = nextTheta - preTheta;
            if (thetaDiff < 0) {
                addLineCap(nowPoint, preTheta - FMath.PiHalf, thetaDiff, previousEndL, startL);
                triangles.add(nowPoint, previousEndR, startR);
            } else {
                addLineCap(nowPoint, preTheta - FMath.PiHalf, thetaDiff, previousEndR, startR);
                triangles.add(nowPoint, previousEndL, startL);
            }

            segmentQuadStartOffsets[i - 1] = triangles.length;
            addLineQuads();
            nowPoint = nextPoint;
            preTheta = nextTheta;
        }
        addLineCap(path.get(max_i - 1), preTheta - FMath.PiHalf, FMath.Pi, endR, endL);
    }


}
