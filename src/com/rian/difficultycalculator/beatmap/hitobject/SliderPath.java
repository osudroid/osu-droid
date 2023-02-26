package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.utils.PathApproximator;
import com.rian.difficultycalculator.math.Precision;
import com.rian.difficultycalculator.math.Vector2;

import java.util.ArrayList;

/**
 * Represents the path of a slider.
 */
public class SliderPath {
    /**
     * The path type of this slider.
     */
    public final PathType pathType;

    /**
     * The control points (anchor points) of this slider path.
     */
    public final ArrayList<Vector2> controlPoints;

    /**
     * The distance that is expected when calculating slider path.
     */
    public final double expectedDistance;

    /**
     * The calculated path of this slider path.
     */
    public final ArrayList<Vector2> calculatedPath = new ArrayList<>();

    /**
     * The cumulative length of this slider path.
     */
    public final ArrayList<Double> cumulativeLength = new ArrayList<>();

    /**
     * @param type The path type of this slider.
     * @param controlPoints The control points (anchor points) of this slider path.
     * @param expectedDistance The distance that is expected when calculating slider path.
     */
    public SliderPath(PathType type, ArrayList<Vector2> controlPoints, double expectedDistance) {
        this.pathType = type;
        this.controlPoints = controlPoints;
        this.expectedDistance = expectedDistance;

        calculatePath();
        calculateCumulativeLength();
    }

    /**
     * Calculates the path of this slider.
     */
    public void calculatePath() {
        calculatedPath.clear();

        int spanStart = 0;

        for (int i = 0; i < controlPoints.size(); ++i) {
            if (i == controlPoints.size() - 1 || controlPoints.get(i).equals(controlPoints.get(i + 1))) {
                int spanEnd = i + 1;
                ArrayList<Vector2> cpSpan = (ArrayList<Vector2>) controlPoints.subList(spanStart, spanEnd);

                calculateSubPath(cpSpan).forEach(t -> {
                    if (calculatedPath.size() == 0 || !calculatedPath.get(calculatedPath.size() - 1).equals(t)) {
                        this.calculatedPath.add(t);
                    }
                });

                spanStart = spanEnd;
            }
        }
    }

    /**
     * Calculates the cumulative length of this slider.
     */
    public void calculateCumulativeLength() {
        cumulativeLength.clear();
        cumulativeLength.add(0d);

        double calculatedLength = 0;

        for (int i = 0; i < calculatedPath.size() - 1; ++i) {
            Vector2 diff = calculatedPath.get(i + 1).subtract(calculatedPath.get(i));
            calculatedLength += diff.getLength();
            cumulativeLength.add(calculatedLength);
        }

        if (calculatedLength != expectedDistance) {
            // In osu-stable, if the last two control points of a slider are equal, extension is not performed.
            if (
                    controlPoints.size() >= 2 &&
                    controlPoints.get(controlPoints.size() - 1)
                            .equals(controlPoints.get(controlPoints.size() - 2)) &&
                    expectedDistance > calculatedLength) {
                return;
            }

            // The last length is always incorrect.
            cumulativeLength.remove(cumulativeLength.size() - 1);
            int pathEndIndex = calculatedPath.size() - 1;

            if (calculatedLength > expectedDistance) {
                // The path will be shortened further, in which case we should trim any more unnecessary lengths and their associated path segments
                while (cumulativeLength.size() > 0 && cumulativeLength.get(cumulativeLength.size() - 1) >= expectedDistance) {
                    cumulativeLength.remove(cumulativeLength.size() - 1);
                    calculatedPath.remove(pathEndIndex--);
                }
            }

            if (pathEndIndex <= 0) {
                // The expected distance is negative or zero
                cumulativeLength.add(0d);
                return;
            }

            // The direction of the segment to shorten or lengthen
            Vector2 dir = calculatedPath.get(pathEndIndex).subtract(calculatedPath.get(pathEndIndex - 1));
            dir.normalize();

            calculatedPath.set(
                    pathEndIndex,
                    calculatedPath.get(pathEndIndex - 1).add(
                            dir.scale(
                                    expectedDistance - cumulativeLength.get(cumulativeLength.size() - 1)
                            )
                    )
            );

            cumulativeLength.add(expectedDistance);
        }
    }

    /**
     * Computes the position on the slider at a given progress that ranges from 0
     * (beginning of the path) to 1 (end of the path).
     *
     * @param progress Ranges from 0 (beginning of the path) to 1 (end of the path).
     */
    public Vector2 positionAt(double progress) {
        double d = progressToDistance(progress);

        return interpolateVertices(indexOfDistance(d), d);
    }

    private ArrayList<Vector2> calculateSubPath(ArrayList<Vector2> subControlPoints) {
        switch (pathType) {
            case Linear:
                return PathApproximator.approximateLinear(subControlPoints);
            case PerfectCurve:
                if (subControlPoints.size() != 3) {
                    break;
                }

                return PathApproximator.approximateCircularArc(subControlPoints);
            case Catmull:
                return PathApproximator.approximateCatmull(subControlPoints);
        }

        return PathApproximator.approximateBezier(subControlPoints);
    }

    /**
     * Returns the progress of reaching expected distance.
     */
    private double progressToDistance(double progress) {
        return Math.min(Math.max(progress, 0), 1) * expectedDistance;
    }

    /**
     * Interpolates verticles of the slider.
     */
    private Vector2 interpolateVertices(int i, double d) {
        if (calculatedPath.size() == 0) {
            return new Vector2(0, 0);
        }

        if (i <= 0) {
            return calculatedPath.get(0);
        }
        if (i >= calculatedPath.size()) {
            return calculatedPath.get(calculatedPath.size() - 1);
        }

        Vector2 p0 = calculatedPath.get(i - 1);
        Vector2 p1 = calculatedPath.get(i);

        double d0 = cumulativeLength.get(i - 1);
        double d1 = cumulativeLength.get(i);

        // Avoid division by and almost-zero number in case two points are extremely close to each other.
        if (Precision.almostEqualsNumber(d0, d1)) {
            return p0;
        }

        double w = (d - d0) / (d1 - d0);
        return p0.add(p1.subtract(p0).scale(w));
    }

    /**
     * Binary searches the cumulative length array and returns the
     * index at which <code>arr[index] >= d</code>.
     *
     * @param d The distance to search.
     * @return The index.
     */
    private int indexOfDistance(double d) {
        if (cumulativeLength.size() == 0 ||d < cumulativeLength.get(0)) {
            return 0;
        }

        if (d >= cumulativeLength.get(cumulativeLength.size() - 1)) {
            return cumulativeLength.size();
        }

        int l = 0;
        int r = cumulativeLength.size() - 2;

        while (l <= r) {
            int pivot = l + ((r - l) >> 1);

            if (cumulativeLength.get(pivot) < d) {
                l = pivot + 1;
            } else if (cumulativeLength.get(pivot) > d) {
                r = pivot - 1;
            } else {
                return pivot;
            }
        }

        return l;
    }

    /**
     * Types of slider paths.
     */
    public enum PathType {
        Catmull,
        Bezier,
        Linear,
        PerfectCurve
    }
}
