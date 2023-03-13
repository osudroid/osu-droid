package com.rian.difficultycalculator.utils;

import com.rian.difficultycalculator.math.Precision;
import com.rian.difficultycalculator.math.Vector2;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Helper methods to approximate a path by interpolating a sequence of control points.
 */
public final class PathApproximator {
    /**
     * The amount of pieces to calculate for each control point quadruplet.
     */
    private static final int catmullDetail = 50;

    private static final float bezierTolerance = 0.25f;
    private static final float circularArcTolerance = 0.1f;

    private PathApproximator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Approximates a bezier slider's path.
     * <br><br>
     * Creates a piecewise-linear approximation of a bezier curve by adaptively repeatedly subdividing
     * the control points until their approximation error vanishes below a given threshold.
     *
     * @param controlPoints The anchor points of the slider.
     */
    public static ArrayList<Vector2> approximateBezier(ArrayList<Vector2> controlPoints) {
        ArrayList<Vector2> output = new ArrayList<>();
        int count = controlPoints.size() - 1;

        if (count < 0) {
            return output;
        }

        // "toFlatten" contains all the curves which are not yet approximated well enough.
        // We use a stack to emulate recursion without the risk of running into a stack overflow.
        // (More specifically, we iteratively and adaptively refine our curve with a
        // depth-first search (https://en.wikipedia.org/wiki/Depth-first_search)
        // over the tree resulting from the subdivisions we make.)
        Stack<Vector2[]> toFlatten = new Stack<>();
        Stack<Vector2[]> freeBuffers = new Stack<>();

        Vector2[] points = new Vector2[controlPoints.size()];
        toFlatten.push(controlPoints.toArray(points));

        Vector2[] subdivisionBuffer1 = new Vector2[count + 1];
        Vector2[] subdivisionBuffer2 = new Vector2[count * 2 + 1];

        while (toFlatten.size() > 0) {
            Vector2[] parent = toFlatten.pop();

            if (bezierIsFlatEnough(parent)) {
                // If the control points we currently operate on are sufficiently "flat", we use
                // an extension to De Casteljau's algorithm to obtain a piecewise-linear approximation
                // of the bezier curve represented by our control points, consisting of the same amount
                // of points as there are control points.
                bezierApproximate(parent, output, subdivisionBuffer1, subdivisionBuffer2, count + 1);
                freeBuffers.push(parent);
                continue;
            }

            // If we do not yet have a sufficiently "flat" (in other words, detailed) approximation we keep
            // subdividing the curve we are currently operating on.
            Vector2[] rightChild = freeBuffers.size() > 0 ? freeBuffers.pop() : new Vector2[count + 1];

            bezierSubdivide(parent, subdivisionBuffer2, rightChild, subdivisionBuffer1, count + 1);

            // We re-use the buffer of the parent for one of the children, so that we save one allocation per iteration.
            if (count + 1 >= 0) {
                System.arraycopy(subdivisionBuffer2, 0, parent, 0, count + 1);
            }

            toFlatten.push(rightChild);
            toFlatten.push(parent);
        }

        output.add(controlPoints.get(count));
        return output;
    }

    /**
     * Approximates a catmull slider's path.
     * <br><br>
     * Creates a piecewise-linear approximation of a Catmull-Rom spline.
     *
     * @param controlPoints The anchor points of the slider.
     */
    public static ArrayList<Vector2> approximateCatmull(ArrayList<Vector2> controlPoints) {
        ArrayList<Vector2> result = new ArrayList<>();

        for (int i = 0; i < controlPoints.size() - 1; ++i) {
            Vector2 v1 = i > 0 ? controlPoints.get(i - 1) : controlPoints.get(i);
            Vector2 v2 = controlPoints.get(i);
            Vector2 v3 = controlPoints.get(i + 1);
            Vector2 v4 = i < controlPoints.size() - 2 ? controlPoints.get(i + 2) : v3.add(v3).subtract(v2);

            for (int c = 0; c < catmullDetail; ++c) {
                result.add(catmullFindPoint(v1, v2, v3, v4, (float) c / catmullDetail));
                result.add(catmullFindPoint(v1, v2, v3, v4, (float) (c + 1) / catmullDetail));
            }
        }

        return result;
    }

    /**
     * Approximates a slider's circular arc.
     * <br><br>
     * Creates a piecewise-linear approximation of a circular arc curve.
     *
     * @param controlPoints The anchor points of the slider.
     */
    public static ArrayList<Vector2> approximateCircularArc(ArrayList<Vector2> controlPoints) {
        Vector2 a = controlPoints.get(0);
        Vector2 b = controlPoints.get(1);
        Vector2 c = controlPoints.get(2);

        // If we have a degenerate triangle where a side-length is almost zero, then give up and fall
        // back to a more numerically stable method.
        if (Precision.almostEqualsNumber(0, (b.y - a.y) * (c.x - a.x) - (b.x - a.x) * (c.y - a.y))) {
            return approximateBezier(controlPoints);
        }

        // See: https://en.wikipedia.org/wiki/Circumscribed_circle#Cartesian_coordinates_2
        float d = 2 * (float) (a.x * b.subtract(c).y + b.x * c.subtract(a).y + c.x * a.subtract(b).y);
        float aSq = (float) Math.pow(a.getLength(), 2);
        float bSq = (float) Math.pow(b.getLength(), 2);
        float cSq = (float) Math.pow(c.getLength(), 2);

        Vector2 center = new Vector2(
                aSq * b.subtract(c).y + bSq * c.subtract(a).y + cSq * a.subtract(b).y,
                aSq * c.subtract(b).x + bSq * a.subtract(c).x + cSq * b.subtract(a).x
        ).divide(d);

        Vector2 dA = a.subtract(center);
        Vector2 dC = c.subtract(center);

        float r = (float) dA.getLength();

        double thetaStart = Math.atan2(dA.y, dA.x);
        double thetaEnd = Math.atan2(dC.y, dC.x);

        while (thetaEnd < thetaStart) {
            thetaEnd += 2 * Math.PI;
        }

        double dir = 1;
        double thetaRange = thetaEnd - thetaStart;

        // Decide in which direction to draw the circle, depending on which side of
        // AC B lies.
        Vector2 orthoAtoC = c.subtract(a);
        orthoAtoC = new Vector2(orthoAtoC.y, -orthoAtoC.x);
        if (orthoAtoC.dot(b.subtract(a)) < 0) {
            dir = -dir;
            thetaRange = 2 * Math.PI - thetaRange;
        }

        // We select the amount of points for the approximation by requiring the discrete curvature
        // to be smaller than the provided tolerance. The exact angle required to meet the tolerance
        // is: 2 * Math.acos(1 - TOLERANCE / r)
        // The special case is required for extremely short sliders where the radius is smaller than
        // the tolerance. This is a pathological rather than a realistic case.
        int amountPoints = 2 * r <= circularArcTolerance
                ? 2
                : (int) Math.max(2, Math.ceil(thetaRange / (2 * Math.acos(1 - circularArcTolerance / r))));

        ArrayList<Vector2> output = new ArrayList<>();

        for (int i = 0; i < amountPoints; ++i) {
            double fraction = (double) i / (amountPoints - 1);
            double theta = thetaStart + dir * fraction * thetaRange;
            Vector2 o = new Vector2(Math.cos(theta), Math.sin(theta)).scale(r);
            output.add(center.add(o));
        }

        return output;
    }

    /**
     * Approximates a linear slider's path.
     * <br><br>
     * Creates a piecewise-linear approximation of a linear curve.
     * Basically, returns the input.
     *
     * @param controlPoints The anchor points of the slider.
     */
    public static ArrayList<Vector2> approximateLinear(ArrayList<Vector2> controlPoints) {
        return controlPoints;
    }

    /**
     * Checks if a bezier slider is flat enough to be approximated.
     * <br><br>
     * Make sure the 2nd order derivative (approximated using finite elements) is within tolerable bounds.
     * <br><br>
     * NOTE: The 2nd order derivative of a 2D curve represents its curvature, so intuitively this function
     * checks (as the name suggests) whether our approximation is <i>locally</i> "flat". More curvy parts
     * need to have a denser approximation to be more "flat".
     *
     * @param controlPoints The anchor points of the slider.
     */
    private static boolean bezierIsFlatEnough(Vector2[] controlPoints) {
        for (int i = 1; i < controlPoints.length - 1; ++i) {
            Vector2 prev = controlPoints[i - 1];
            Vector2 current = controlPoints[i];
            Vector2 next = controlPoints[i + 1];

            Vector2 finalVec = prev.subtract(current.scale(2)).add(next);

            if (Math.pow(finalVec.getLength(), 2) > Math.pow(bezierTolerance, 2) * 4) {
                return false;
            }
        }

        return true;
    }

    /**
     * Approximates a bezier slider's path.
     * <br><br>
     * This uses <a href="https://en.wikipedia.org/wiki/De_Casteljau%27s_algorithm">De Casteljau's algorithm</a> to obtain an optimal
     * piecewise-linear approximation of the bezier curve with the same amount of points as there are control points.
     *
     * @param controlPoints The control points describing the bezier curve to be approximated.
     * @param output The points representing the resulting piecewise-linear approximation.
     * @param subdivisionBuffer1 The first buffer containing the current subdivision state.
     * @param subdivisionBuffer2 The second buffer containing the current subdivision state.
     * @param count The number of control points in the original array.
     */
    private static void bezierApproximate(Vector2[] controlPoints, ArrayList<Vector2> output,
                                          Vector2[] subdivisionBuffer1, Vector2[] subdivisionBuffer2,
                                          int count) {
        bezierSubdivide(controlPoints, subdivisionBuffer2, subdivisionBuffer1, subdivisionBuffer1, count);

        if (count - 1 >= 0) {
            System.arraycopy(subdivisionBuffer1, 1, subdivisionBuffer2, count, count - 1);
        }

        output.add(controlPoints[0]);

        for (int i = 1; i < count - 1; ++i) {
            int index = 2 * i;
            Vector2 p = subdivisionBuffer2[index - 1]
                    .add(subdivisionBuffer2[index].scale(2))
                    .add(subdivisionBuffer2[index + 1])
                    .scale(0.25f);
            output.add(p);
        }
    }

    /**
     * Subdivides <code>n</code> control points representing a bezier curve into 2 sets of <code>n</code>
     * control points, each describing a bezier curve equivalent to a half of the original curve.
     * Effectively this splits the original curve into 2 curves which result in the original curve
     * when pieced back together.
     *
     * @param controlPoints The anchor points of the slider.
     * @param l Parts of the slider for approximation.
     * @param r Parts of the slider for approximation.
     * @param subdivisionBuffer Parts of the slider for approximation.
     * @param count The amount of anchor points in the slider.
     */
    private static void bezierSubdivide(Vector2[] controlPoints, Vector2[] l, Vector2[] r,
                                        Vector2[] subdivisionBuffer, int count) {
        if (count >= 0) {
            System.arraycopy(controlPoints, 0, subdivisionBuffer, 0, count);
        }

        for (int i = 0; i < count; ++i) {
            l[i] = subdivisionBuffer[0];
            r[count - i - 1] = subdivisionBuffer[count - i - 1];

            for (int j = 0; j < count - i - 1; ++j) {
                subdivisionBuffer[j] = subdivisionBuffer[j].add(subdivisionBuffer[j + 1]).divide(2);
            }
        }
    }

    /**
     * Finds a point on the spline at the position of a parameter.
     *
     * @param vec1 The first vector.
     * @param vec2 The second vector.
     * @param vec3 The third vector.
     * @param vec4 The fourth vector.
     * @param t The parameter at which to find the point on the spline, in the range [0, 1].
     */
    private static Vector2 catmullFindPoint(Vector2 vec1, Vector2 vec2,
                                            Vector2 vec3, Vector2 vec4, float t) {
        float t2 = (float) Math.pow(t, 2);
        float t3 = (float) Math.pow(t, 3);

        return new Vector2(
                0.5f *
                        (2 * vec2.x +
                                (-vec1.x + vec3.x) * t +
                                (2 * vec1.x - 5 * vec2.x + 4 * vec3.x - vec4.x) * t2 +
                                (-vec1.x + 3 * vec2.x - 3 * vec3.x + vec4.x) * t3),
                0.5f *
                        (2 * vec2.y +
                                (-vec1.y + vec3.y) * t +
                                (2 * vec1.y - 5 * vec2.y + 4 * vec3.y - vec4.y) * t2 +
                                (-vec1.y + 3 * vec2.y - 3 * vec3.y + vec4.y) * t3)
        );
    }
}

