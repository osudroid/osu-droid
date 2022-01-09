package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.edlplan.osu.support.timing.controlpoint.ControlPoints;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.osu.polygon.Spline;

public class GameHelper {
    public static ControlPoints controlPoints;
    private static double binomTable[][];
    private static int binomTableN;
    private static float tickRate = 1;
    private static float scale = 1;
    private static float speed = 1;
    private static float difficulty = 1;
    private static float approachRate = 1;
    private static float drain = 0;
    private static float stackLatient = 0;
    private static float timeMultiplier = 0;
    private static RGBColor sliderColor = new RGBColor();
    private static boolean hidden = false;
    private static boolean flashLight = false;
    private static boolean hardrock = false;
    private static boolean relaxMod = false;
    private static boolean doubleTime = false;
    private static boolean nightCore = false;
    private static boolean speedUp = false;
    private static boolean halfTime = false;
    private static boolean autopilotMod = false;
    private static boolean suddenDeath = false;
    private static boolean perfect = false;
    private static boolean useReplay;
    private static boolean isKiai = false;
    private static boolean auto = false;
    private static float beatLength = 0;
    private static float timingOffset = 0;
    private static int timeSignature = 4;
    private static float initalBeatLength = 0;
    private static float globalTime = 0;
    private static Spline.CurveTypes curveType;
    private static int gameid = 0;
    private static Queue<SliderPath> pathPool = new LinkedList<GameHelper.SliderPath>();
    private static Queue<PointF> pointPool = new LinkedList<PointF>();

    private static DifficultyHelper difficultyHelper = DifficultyHelper.StdDifficulty;

    static {
        binomTableN = 35;
        binomTable = new double[binomTableN][binomTableN];
        for (int n = 0; n < binomTableN; n++) {
            for (int k = 0; k < binomTableN; k++) {
                if (n == 0) {
                    binomTable[n][k] = 0;
                    continue;
                }
                if (k == 0) {
                    binomTable[n][k] = 1;
                    continue;
                }
                binomTable[n][k] = 1;
                for (int i = 1; i <= k; i++) {
                    binomTable[n][k] *= (n - k + i) / (double) i;
                }
            }
        }
    }

    public static DifficultyHelper getDifficultyHelper() {
        return difficultyHelper;
    }

    public static void setDifficultyHelper(DifficultyHelper difficultyHelper) {
        GameHelper.difficultyHelper = difficultyHelper;
    }

    public static float getDrain() {
        return drain;
    }

    public static void setDrain(final float drain) {
        GameHelper.drain = drain;
    }

    public static float getDifficulty() {
        return difficulty;
    }

    public static void setDifficulty(final float difficulty) {
        GameHelper.difficulty = difficulty;
    }

    public static float getTickRate() {
        return tickRate;
    }

    public static void setTickRate(final float tickRate) {
        GameHelper.tickRate = tickRate;
    }

    public static int getGameid() {
        return gameid;
    }

    public static void updateGameid() {
        gameid = (new Random().nextInt(233333333) + 1);
    }

    public static PointF getBezier(final float t, final ArrayList<PointF> points) {
        final PointF b = newPointF();
        b.set(0, 0);
        final int n = points.size() - 1;
        // This is formula for Bezier curve from wiki:
        // B(t) = Sum[i = 0..n]( Bi(t) * Pi )
        for (int i = 0; i <= n; i++) {
            // Bi(t) = C(n,i) * t^i * (1-t)^(n-i)
            final double bi = binomial(n, i) * Math.pow(t, i)
                    * Math.pow(1 - t, n - i);
            b.x += points.get(i).x * bi;
            b.y += points.get(i).y * bi;
        }

        return b;
    }

    private static double binomial(final int n, final int k) {
        if (n <= 0) {
            return 0;
        }
        if (k <= 0) {
            return 1;
        }
        if (n < binomTableN && k < binomTableN) {
            return binomTable[n][k];
        } else {
            return binomial(n - 1, k - 1) + binomial(n - 1, k);
        }
    }

    public static ArrayList<PointF> parseSection(
            final ArrayList<PointF> rawPoints) {
        final ArrayList<PointF> result = new ArrayList<PointF>();
        final PointF pos = rawPoints.get(0);
        final PointF endpos = rawPoints.get(rawPoints.size() - 1);

        if (rawPoints.size() < 2) {
            result.add(pos);
            return result;
        }

        result.add(pos);
        result.add(getBezier(0.5f, rawPoints));
        result.add(endpos);
        int index = 0;

        final ArrayList<Float> ts = new ArrayList<Float>();
        ts.add(0f);
        ts.add(0.5f);
        ts.add(1F);
        float step2 = Constants.SLIDER_STEP * scale;
//		if (Config.isLowpolySliders() == false) {
//			step2 = Constants.HIGH_SLIDER_STEP * scale;
//		}
        step2 *= step2;
        while (index < result.size() - 1) {
            while (Utils.squaredDistance(result.get(index).x,
                    result.get(index).y, result.get(index + 1).x,
                    result.get(index + 1).y) > step2) {
                final float t = (ts.get(index) + ts.get(index + 1)) / 2f;
                result.add(index + 1, getBezier(t, rawPoints));
                ts.add(index + 1, t);
            }
            index++;
        }

        return result;
    }

    public static SliderPath calculatePath(final PointF pos,
                                           final String[] data, final float maxLength, final float offset) {
        final ArrayList<ArrayList<PointF>> points = new ArrayList<ArrayList<PointF>>();
        points.add(new ArrayList<PointF>());
        int lastIndex = 0;
        points.get(lastIndex).add(pos);

        final SliderPath path = newPath();

        for (final String s : data) {
            if (s == data[0]) {
                curveType = Spline.getCurveType(s.charAt(0));
                continue;
            }
            final String[] nums = s.split("[:]");
            final PointF point = newPointF();
            point.set(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
            point.x += offset;
            point.y += offset;
            final PointF ppoint = points.get(lastIndex).get(
                    points.get(lastIndex).size() - 1);
            if (point.x == ppoint.x && point.y == ppoint.y
                    || (Config.isAccurateSlider() == false && (data[0].equals("L") || data[0].equals("C")))) {
                if (Config.isAccurateSlider() == false && (data[0].equals("L") || data[0].equals("C"))) {
                    points.get(lastIndex).add(point);
                }
                points.add(new ArrayList<PointF>());
                lastIndex++;
            }
            points.get(lastIndex).add(point);
        }

        ArrayList<PointF> section;
        int pind = -1;
        float trackLength = 0;
        final PointF vec = newPointF();

        MainCycle:
        for (final ArrayList<PointF> plist : points) {
            if (Config.isAccurateSlider() == true) {
                final Spline spline = Spline.getInstance();
                spline.setControlPoints(plist);
                spline.setType(curveType);
                spline.Refresh();
                section = spline.getPoints();
            } else {
                section = parseSection(plist);
            }
            // Debug.i("section size=" + section.size());
            boolean firstPut = false;
            for (final PointF p : section) {
                if (pind < 0
                        || Math.abs(p.x - path.points.get(pind).x)
                        + Math.abs(p.y - path.points.get(pind).y) > 1f) {
                    if (firstPut == false) {
                        firstPut = true;
                        path.boundIndexes.add(path.points.size());
                    }
                    if (path.points.isEmpty() == false) {
                        vec.set(p.x - path.points.get(path.points.size() - 1).x,
                                p.y - path.points.get(path.points.size() - 1).y);
                        trackLength += Utils.length(vec);
                        path.length.add(trackLength);
                    }
                    path.points.add(p);
                    pind++;

                    if (trackLength >= maxLength) {
                        if (path.points.isEmpty() == false) {
                            path.boundIndexes.add(path.points.size() - 1);
                        }
                        break MainCycle;
                    }
                }
            }
            if (path.points.isEmpty() == false) {
                path.boundIndexes.add(path.points.size() - 1);
            }
        }

        for (int i = 0; i < path.points.size(); i++) {
            path.points.set(i, Utils.trackToRealCoords(path.points.get(i)));
        }

        if (path.points.size() == 1) {
            path.points.add(new PointF(path.points.get(0).x,
                    path.points.get(0).y));
            path.length.add(0f);
        }

        return path;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(final float scale) {
        GameHelper.scale = scale;
    }

    public static float getApproachRate() {
        return approachRate;
    }

    public static void setApproachRate(float approachRate) {
        GameHelper.approachRate = approachRate;
    }

    public static float getTimeMultiplier() {
        return timeMultiplier;
    }

    public static void setTimeMultiplier(float timeMultiplier) {
        GameHelper.timeMultiplier = timeMultiplier;
    }

    public static float getSpeed() {
        return speed;
    }

    public static void setSpeed(final float speed) {
        GameHelper.speed = speed;
    }

    public static void putPath(final SliderPath path) {
        for (final PointF p : path.points) {
            pointPool.add(p);
        }
        path.points.clear();
        path.length.clear();
        path.boundIndexes.clear();
        pathPool.add(path);
    }

    private static SliderPath newPath() {
        if (pathPool.isEmpty()) {
            return new SliderPath();
        }
        return pathPool.poll();
    }

    private static PointF newPointF() {
        if (pointPool.isEmpty()) {
            return new PointF();
        }
        return pointPool.poll();
    }

    public static float getStackLatient() {
        return stackLatient;
    }

    public static void setStackLatient(final float stackLatient) {
        GameHelper.stackLatient = stackLatient;
    }

    public static RGBColor getSliderColor() {
        return sliderColor;
    }

    public static void setSliderColor(final RGBColor sliderColor) {
        GameHelper.sliderColor = sliderColor;
    }

    public static boolean isHardrock() {
        return hardrock;
    }

    public static void setHardrock(final boolean hardrock) {
        GameHelper.hardrock = hardrock;
    }

    public static boolean isHidden() {
        return hidden;
    }

    public static void setHidden(final boolean hidden) {
        GameHelper.hidden = hidden;
    }

    public static boolean isFlashLight() {
        return flashLight;
    }

    public static void setFlashLight(final boolean flashLight) {
        GameHelper.flashLight = flashLight;
    }
    public static boolean isHalfTime() {
        return halfTime;
    }

    public static void setHalfTime(final boolean halfTime) {
        GameHelper.halfTime = halfTime;
    }

    public static boolean isNightCore() {
        return nightCore;
    }

    public static void setNightCore(final boolean nightCore) {
        GameHelper.nightCore = nightCore;
    }

    public static boolean isDoubleTime() {
        return doubleTime;
    }

    public static void setDoubleTime(final boolean doubleTime) {
        GameHelper.doubleTime = doubleTime;
    }

    public static boolean isSpeedUp() {
        return speedUp;
    }

    public static void setSpeedUp(final boolean speedUp) {
        GameHelper.speedUp = speedUp;
    }
    public static boolean isSuddenDeath() {
        return suddenDeath;
    }

    public static void setSuddenDeath(final boolean suddenDeath) {
        GameHelper.suddenDeath = suddenDeath;
    }

    public static boolean isPerfect() {
        return perfect;
    }

    public static void setPerfect(final boolean perfect) {
        GameHelper.perfect = perfect;
    }

    public static boolean isUseReplay() {
        return useReplay;
    }

    public static void setUseReplay(final boolean useReplay) {
        GameHelper.useReplay = useReplay;
    }

    public static boolean isKiai() {
        if (OsuSkin.get().isDisableKiai()) return false;
        return isKiai;
    }

    public static void setKiai(final boolean isKiai) {
        GameHelper.isKiai = isKiai;
    }

    public static float getGlobalTime() {
        return globalTime;
    }

    public static void setGlobalTime(final float globalTime) {
        GameHelper.globalTime = globalTime;
    }

    public static float getBeatLength() {
        return beatLength;
    }

    public static void setBeatLength(final float beatLength) {
        GameHelper.beatLength = beatLength;
    }

    public static float getTimingOffset() {
        return timingOffset;
    }

    public static void setTimingOffset(float timingOffset) {
        GameHelper.timingOffset = timingOffset;
    }

    public static int getTimeSignature() {
        return timeSignature;
    }

    public static void setTimeSignature(int timeSignature) {
        GameHelper.timeSignature = timeSignature;
    }

    public static float getInitalBeatLength() {
        return initalBeatLength;
    }

    public static void setInitalBeatLength(float initalBeatLength) {
        GameHelper.initalBeatLength = initalBeatLength;
    }

    public static float getSliderTickLength() {
        return 100f * initalBeatLength / speed;
    }

    public static float getKiaiTickLength() {
        return initalBeatLength;
    }

    public static boolean isRelaxMod() {
        return relaxMod;
    }

    public static void setRelaxMod(boolean relaxMod) {
        GameHelper.relaxMod = relaxMod;
    }

    public static boolean isAutopilotMod() {
        return autopilotMod;
    }

    public static void setAutopilotMod(boolean autopilotMod) {
        GameHelper.autopilotMod = autopilotMod;
    }

    public static boolean isAuto() {
        return auto;
    }

    public static void setAuto(boolean auto) {
        GameHelper.auto = auto;
    }

    public static float Round(double value, int digits) throws NumberFormatException {
        if (Math.abs(value) < Double.MAX_VALUE) {
            float f1 = 0;
            BigDecimal b = new BigDecimal(value);
            f1 = b.setScale(digits, RoundingMode.HALF_UP).floatValue();
            return f1;
        } else {
            return value > 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }
    }

    public static double ar2ms(double ar) {
        return Round((ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar), 0);
    }

    public static double ms2ar(double ms) {
        return (ms <= 1200) ? ((1200 - ms) / 150.0 + 5) : (1800 - ms) / 120.0;
    }

    public static double ms2od(double ms) {
        return (80 - ms) / 6.0;
    }

    public static double od2ms(double od) {
        return Round(80 - od * 6, 1);
    }

    public static class SliderPath {
        public ArrayList<PointF> points = new ArrayList<PointF>();
        public ArrayList<Float> length = new ArrayList<Float>();
        public ArrayList<Integer> boundIndexes = new ArrayList<Integer>();
    }
}
