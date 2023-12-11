package ru.nsu.ccfit.zuev.osu.polygon;

import android.graphics.PointF;

import java.util.ArrayList;

/**
 * Created by Fuuko on 2015/4/7.
 */
public class Spline {

    private static final float TWO_PI = (float) (Math.PI * 2);

    private static final int DetailLevel = 50;

    private static Spline instance = null;

    private final ArrayList<PointF> m_ctrl_pts;

    private final ArrayList<Line> m_path;

    private final ArrayList<PointF> m_points;

    private CurveTypes m_curve_type;

    private ArrayList<PointF> m_points_copy = null;

    public Spline() {
        m_ctrl_pts = new ArrayList<>();
        m_curve_type = CurveTypes.Linear;
        m_path = new ArrayList<>();
        m_points = new ArrayList<>();
    }

    public static Spline getInstance() {
        if (instance == null) {
            instance = new Spline();
        }
        return instance;
    }

    private static float DistToOrigin(PointF point) {
        return (float) Math.sqrt(point.x * point.x + point.y * point.y);
    }

    private static float Rho(Line line) {
        return DistToOrigin(Subtract(line.p2, line.p1));
    }

    /**
     * /// Copied from peppy's slider fragment code. (substituting XNA Vector2s for System.Drawing.PointFs)
     */
    private static ArrayList<PointF> CreateBezier(ArrayList<PointF> input) {
        float DetailLevel2 = (float) DetailLevel;
        PointF[] working = new PointF[input.size()];
        ArrayList<PointF> output = new ArrayList<>();

        PointF lll;

        for (int iteration = 0; iteration <= DetailLevel; iteration++) {
            // Reset the control points for the next sample
            for (int i = 0; i < input.size(); i++) {
                working[i] = new PointF(input.get(i).x, input.get(i).y);
            }

            // I love how you unrolled the recursion from the Bezier formula, peppy ^o^
            for (int level = 0; level < input.size() - 1; level++) {
                for (int i = 0; i < input.size() - level - 1; i++) {
                    lll = Lerp(working[i], working[i + 1], (float) iteration / DetailLevel2);
                    working[i] = lll; // fix premature overwriting of out variable before done with it
                }
            }

            output.add(working[0]);
        }

        return output;
    }

    /// <summary>
    /// Linear interpolates between 2 points. Support method from Xna.Vector2 not implemented within PointF.
    /// </summary>
    /// <param name="pt1">Start point</param>
    /// <param name="pt2">End point</param>
    /// <param name="weight">Weight, starting at Point1 = 0 and going to Point2 = 1.</param>
    /// <param name="result">Result</param>
    private static PointF Lerp(PointF pt1, PointF pt2, float weight) {
        // HURR DERP!!
        //result = pt1 + PtToSize(MultiplyPt(pt2 - PtToSize(pt1), weight));
        if ((weight > 1) || (weight < 0)) {
            throw new ArrayIndexOutOfBoundsException("weight");
        }
        //result = new PointF(pt1.x + (pt2.x - pt1.x) * weight, pt1.y + (pt2.y - pt1.y) * weight);
        return Lerp(pt1.x, pt2.x, pt1.y, pt2.y, weight);
    }

    private static PointF Lerp(float x1, float x2, float y1, float y2, float weight) {
        if ((weight > 1) || (weight < 0)) {
            throw new ArrayIndexOutOfBoundsException("weight");
        }
        return new PointF(x1 + (x2 - x1) * weight, y1 + (y2 - y1) * weight);
    }

    private static PointF CatmullRom(PointF value1, PointF value2, PointF value3, PointF value4, float amount) {
        PointF vector = new PointF();
        float num = amount * amount;
        float num2 = amount * num;
        vector.x = 0.5f * ((((2f * value2.x) + ((-value1.x + value3.x) * amount)) + (((((2f * value1.x) - (5f * value2.x)) + (4f * value3.x)) - value4.x) * num)) + ((((-value1.x + (3f * value2.x)) - (3f * value3.x)) + value4.x) * num2));
        vector.y = 0.5f * ((((2f * value2.y) + ((-value1.y + value3.y) * amount)) + (((((2f * value1.y) - (5f * value2.y)) + (4f * value3.y)) - value4.y) * num)) + ((((-value1.y + (3f * value2.y)) - (3f * value3.y)) + value4.y) * num2));
        return vector;
    }

    private static PointF MultiplyPt(PointF value, float scalar) {
        return new PointF(value.x * scalar, value.y * scalar);
    }

    private static PointF CircleCenterPoint(PointF point1, PointF point2, PointF point3) {
        PointF center = new PointF();
        double a = Math.pow(point1.x, 2) + Math.pow(point1.y, 2);
        double b = Math.pow(point2.x, 2) + Math.pow(point2.y, 2);
        double c = Math.pow(point3.x, 2) + Math.pow(point3.y, 2);
        double g = (point3.y - point2.y) * point1.x + (point1.y - point3.y) * point2.x + (point2.y - point1.y) * point3.x;
        center.x = (float) (((b - c) * point1.y + (c - a) * point2.y + (a - b) * point3.y) / (2 * g));
        center.y = (float) (((c - b) * point1.x + (a - c) * point2.x + (b - a) * point3.x) / (2 * g));
        return center;
    }

    private static float TwoPointSide(PointF point1, PointF point2) {
        return (float) Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2));
    }

    private static float CircleRadius(PointF point1, PointF point2, PointF point3) {
        float a = TwoPointSide(point1, point2);
        float b = TwoPointSide(point2, point3);
        float c = TwoPointSide(point1, point3);
        return (float) ((a * b * c) / Math.sqrt((a + b + c) * (a + b - c) * (a - b + c) * (-a + b + c)));
    }

    private static PointF CircularArc(float startAng, float endAng, PointF circleCenter, float radius, float t) {
        PointF vector = new PointF();
        float ang = lerp(startAng, endAng, t);
        vector.x = (float) (Math.cos(ang) * radius + circleCenter.x);
        vector.y = (float) (Math.sin(ang) * radius + circleCenter.y);
        return vector;
    }

    private static boolean isIn(float a, float b, float c) {
        return (b > a && b < c) || (b < a && b > c);
    }

        /*
        private Point GetLineCoords(ArrayList<Point> points, int length)
        {
            // Find the last segment before length:
            ArrayList<double> lengths2;
            ArrayList<double> lengths1 = GetLineLengths(points, out lengths2);
            int index = lengths2.FindLastIndex(delegate(double d) { return d > length; });
        }

        private ArrayList<double> GetLineLengths(ArrayList<Point> points, out ArrayList<double> accumu)
        {
            ArrayList<double> result = new ArrayList<double>();
            accumu = new ArrayList<double>();
            double accu_value = 0D;
            for (int x = 1; x < points.Count; x++)
            {
                result.Add(DistToOrigin(points[x] - points[x - 1]));
                accumu.Add(accu_value += DistToOrigin(points[x] - points[x - 1]));
            }
            return result;
        }

        private static Point GetBezierCoords(ArrayList<Point> points, int length)
        {
        }
        */

    private static float lerp(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    public static PointF Add(PointF pt1, PointF sz2) {
        return new PointF(pt1.x + sz2.x, pt1.y + sz2.y);
    }

    public static PointF Subtract(PointF pt1, PointF sz2) {
        return new PointF(pt1.x - sz2.x, pt1.y - sz2.y);
    }

    public static CurveTypes getCurveType(char c) {
        switch (c) {
            case 'L':
                return CurveTypes.Linear;
            case 'C':
                return CurveTypes.Catmull;
            case 'P':
                return CurveTypes.PerfectCurve;
            case 'B':
            default:
                return CurveTypes.Bezier;
        }
    }

    public void setControlPoints(ArrayList<PointF> theControlPoints) {
        m_ctrl_pts.clear();
        m_ctrl_pts.addAll(theControlPoints);
    }

    public void setType(CurveTypes type) {
        m_curve_type = type;
    }

    public ArrayList<PointF> getPoints() {
        if (m_points_copy == null) {
            m_points_copy = new ArrayList<>(m_points);
        }
        return m_points_copy;
    }

    public void Refresh() {
        m_path.clear();
        m_points.clear();

        sliderthing(m_curve_type, m_ctrl_pts, m_points);

        m_points_copy = null;
    }

    private void sliderthing(CurveTypes CurveType, ArrayList<PointF> sliderCurvePoints, ArrayList<PointF> points) {
        switch (CurveType) {
            case Catmull:
                for (int j = 0; j < sliderCurvePoints.size() - 1; j++) {
                    PointF v1 = (j - 1 >= 0 ? sliderCurvePoints.get(j - 1) : sliderCurvePoints.get(j));
                    PointF v2 = sliderCurvePoints.get(j);
                    PointF v3 = (j + 1 < sliderCurvePoints.size() ? sliderCurvePoints.get(j + 1) : Add(v2, Subtract(v2, v1)));
                    PointF v4 = (j + 2 < sliderCurvePoints.size() ? sliderCurvePoints.get(j + 2) : Add(v3, Subtract(v3, v2)));

                    for (int k = 0; k < DetailLevel; k++) {
                        points.add(CatmullRom(v1, v2, v3, v4, (float) k / DetailLevel));
                    }
                }
                break;

            case Bezier:
                int lastIndex = 0;
                for (int i = 0; i < sliderCurvePoints.size(); i++) {
                    if ((i > 0 && sliderCurvePoints.get(i) == sliderCurvePoints.get(i - 1)) || i == sliderCurvePoints.size() - 1) {
                        ArrayList<PointF> thisLength = new ArrayList<>(sliderCurvePoints.subList(lastIndex, i - lastIndex + ((i == sliderCurvePoints.size() - 1) ? 1 : 0))); // + 1); // i145

                        ArrayList<PointF> points1 = CreateBezier(thisLength);
                        points.addAll(points1);
                        lastIndex = i;
                    }
                }
                break;

            case Linear:
                for (int i = 1; i < sliderCurvePoints.size(); i++) {
                    Line l = new Line(sliderCurvePoints.get(i - 1), sliderCurvePoints.get(i));
                    int segments = (int) (Rho(l) / 10);
                    //                    if (segments == 0) segments = 1; /********FIX for current osu! bug!********/
                    if (segments <= 3) {
                        segments = 5;
                    } /********FIX for current osu! bug!********/
                    // Debug.i("segments=" + segments);
                    for (int j = 0; j < segments; j++) {
                        points.add(Add(l.p1, MultiplyPt(Subtract(l.p2, l.p1), ((float) j / segments))));
                    }
                }
                break;

            case PerfectCurve:
                if (sliderCurvePoints.size() < 3 || (sliderCurvePoints.size() == 3 && ((sliderCurvePoints.get(0).x - sliderCurvePoints.get(2).x) * (sliderCurvePoints.get(1).y - sliderCurvePoints.get(2).y) == (sliderCurvePoints.get(1).x - sliderCurvePoints.get(2).x) * (sliderCurvePoints.get(0).y - sliderCurvePoints.get(2).y)))) {
                    sliderthing(CurveTypes.Linear, m_ctrl_pts, m_points);
                    break;
                }
                PointF point1 = sliderCurvePoints.get(0);
                PointF point2 = sliderCurvePoints.get(1);
                PointF point3 = sliderCurvePoints.get(2);
                PointF circleCenter = CircleCenterPoint(point1, point2, point3);
                float radius = CircleRadius(point1, point2, point3);
                float startAng = (float) Math.atan2(point1.y - circleCenter.y, point1.x - circleCenter.x);
                float midAng = (float) Math.atan2(point2.y - circleCenter.y, point2.x - circleCenter.x);
                float endAng = (float) Math.atan2(point3.y - circleCenter.y, point3.x - circleCenter.x);
                if (!isIn(startAng, midAng, endAng)) {
                    if (Math.abs(startAng + TWO_PI - endAng) < TWO_PI && isIn(startAng + (TWO_PI), midAng, endAng)) {
                        startAng += TWO_PI;
                    } else if (Math.abs(startAng - (endAng + TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng + (TWO_PI))) {
                        endAng += TWO_PI;
                    } else if (Math.abs(startAng - TWO_PI - endAng) < TWO_PI && isIn(startAng - (TWO_PI), midAng, endAng)) {
                        startAng -= TWO_PI;
                    } else if (Math.abs(startAng - (endAng - TWO_PI)) < TWO_PI && isIn(startAng, midAng, endAng - (TWO_PI))) {
                        endAng -= TWO_PI;
                    }
                }
                if (Math.abs(startAng - midAng) < 0.1 && Math.abs(midAng - endAng) < 0.1) {
                    sliderthing(CurveTypes.Bezier, m_ctrl_pts, m_points);
                    break;
                }
                //                points.add(point1);
                for (int k = 0; k < DetailLevel; k++) {
                    points.add(CircularArc(startAng, endAng, circleCenter, radius, (float) k / DetailLevel));
                }
                //                points.add(point3);
                break;
        }
    }

    public enum CurveTypes {
        Linear, Bezier, Catmull, PerfectCurve
    }

    /// <summary>
    /// Probably another XNA class? Guessing its structure based on usage
    /// </summary>
    public static class Line {

        public final PointF p1;

        public final PointF p2;

        public Line(PointF Start, PointF End) {
            p1 = Start;
            p2 = End;
        }

    }

}
