package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;
import com.edlplan.osu.support.timing.controlpoint.ControlPoints;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;
import ru.nsu.ccfit.zuev.osu.polygon.Spline;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class GameHelper {

    private static final Queue<SliderPath> pathPool = new LinkedList<>();

    private static final Queue<PointF> pointPool = new LinkedList<>();

    public static ControlPoints controlPoints;

    private static double tickRate = 1;

    private static float scale = 1;

    private static double speed = 1;

    private static float difficulty = 1;

    private static float approachRate = 1;

    private static float drain = 0;

    private static float stackLeniency = 0;

    private static float timeMultiplier = 0;

    private static RGBColor sliderColor = new RGBColor();

    private static boolean hidden = false;

    private static boolean flashLight = false;

    private static boolean hardrock = false;

    private static boolean relaxMod = false;

    private static boolean doubleTime = false;

    private static boolean nightCore = false;

    private static boolean halfTime = false;

    private static boolean autopilotMod = false;

    private static boolean suddenDeath = false;

    private static boolean perfect = false;

    private static boolean scoreV2;

    private static boolean isEasy;

    private static boolean useReplay;

    private static boolean isKiai = false;

    private static boolean auto = false;

    private static double beatLength = 0;

    private static double timingOffset = 0;

    private static int timeSignature = 4;

    private static double initalBeatLength = 0;

    private static double globalTime = 0;

    private static Spline.CurveTypes curveType;

    private static int gameid = 0;

    private static DifficultyHelper difficultyHelper = DifficultyHelper.StdDifficulty;

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

    public static double getTickRate() {
        return tickRate;
    }

    public static void setTickRate(final double tickRate) {
        GameHelper.tickRate = tickRate;
    }

    public static int getGameid() {
        return gameid;
    }

    public static void updateGameid() {
        gameid = (new Random().nextInt(233333333) + 1);
    }

    public static SliderPath calculatePath(final PointF pos, final String[] data, final float maxLength, final float offset) {
        final ArrayList<ArrayList<PointF>> points = new ArrayList<>();
        points.add(new ArrayList<>());
        int lastIndex = 0;
        points.get(lastIndex).add(pos);

        final SliderPath path = newPath();

        for (final String s : data) {
            if (s.equals(data[0])) {
                curveType = Spline.getCurveType(s.charAt(0));

                if (curveType == Spline.CurveTypes.PerfectCurve && data.length != 3) {
                    // A perfect circle curve must have exactly 3 control points: the initial position, and 2 other control points.
                    // Fallback to a Bézier curve if there are more or less control points.
                    curveType = Spline.CurveTypes.Bezier;
                }

                continue;
            }
            final String[] nums = s.split(":");
            final PointF point = newPointF();
            point.set(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
            point.x += offset;
            point.y += offset;
            final PointF ppoint = points.get(lastIndex).get(points.get(lastIndex).size() - 1);
            if (point.x == ppoint.x && point.y == ppoint.y || data[0].equals("C")) {
                if (data[0].equals("C")) {
                    points.get(lastIndex).add(point);
                }
                points.add(new ArrayList<>());
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
            final Spline spline = Spline.getInstance();
            spline.setControlPoints(plist);
            spline.setType(curveType);
            spline.Refresh();
            section = spline.getPoints();

            // If for some reason a circular arc could not be fit to the 3 given points, fall back to a numerically stable Bézier approximation.
            if (curveType == Spline.CurveTypes.PerfectCurve && section.isEmpty()) {
                spline.setType(Spline.CurveTypes.Bezier);
                spline.Refresh();
                section = spline.getPoints();
            }

            // Debug.i("section size=" + section.size());
            for (final PointF p : section) {
                if (pind < 0 || Math.abs(p.x - path.points.get(pind).x) + Math.abs(p.y - path.points.get(pind).y) > 1f) {
                    if (!path.points.isEmpty()) {
                        vec.set(p.x - path.points.get(path.points.size() - 1).x, p.y - path.points.get(path.points.size() - 1).y);
                        trackLength += Utils.length(vec);
                        path.length.add(trackLength);
                    }
                    path.points.add(p);
                    pind++;

                    if (trackLength >= maxLength) {
                        break MainCycle;
                    }
                }
            }
        }

        for (int i = 0; i < path.points.size(); i++) {
            path.points.set(i, Utils.trackToRealCoords(path.points.get(i)));
        }

        if (path.points.size() == 1) {
            path.points.add(new PointF(path.points.get(0).x, path.points.get(0).y));
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

    public static double getSpeed() {
        return speed;
    }

    public static void setSpeed(final double speed) {
        GameHelper.speed = speed;
    }

    public static void putPath(final SliderPath path) {
        pointPool.addAll(path.points);
        path.points.clear();
        path.length.clear();
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

    public static float getStackLeniency() {
        return stackLeniency;
    }

    public static void setStackLeniency(final float stackLeniency) {
        GameHelper.stackLeniency = stackLeniency;
    }

    public static RGBColor getSliderColor() {
        return sliderColor;
    }

    public static void setSliderColor(final RGBColor sliderColor) {
        GameHelper.sliderColor = sliderColor;
    }

    public static boolean isEasy() {
        return isEasy;
    }

    public static void setEasy(boolean isEasy) {
        GameHelper.isEasy = isEasy;
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

    public static boolean isScoreV2() {
        return scoreV2;
    }

    public static void setScoreV2(boolean scoreV2) {
        GameHelper.scoreV2 = scoreV2;
    }

    public static boolean isUseReplay() {
        return useReplay;
    }

    public static void setUseReplay(final boolean useReplay) {
        GameHelper.useReplay = useReplay;
    }

    public static boolean isKiai() {
        return !OsuSkin.get().isDisableKiai() && isKiai;
    }

    public static void setKiai(final boolean isKiai) {
        GameHelper.isKiai = isKiai;
    }

    public static double getGlobalTime() {
        return globalTime;
    }

    public static void setGlobalTime(final double globalTime) {
        GameHelper.globalTime = globalTime;
    }

    public static double getBeatLength() {
        return beatLength;
    }

    public static void setBeatLength(final double beatLength) {
        GameHelper.beatLength = beatLength;
    }

    public static double getTimingOffset() {
        return timingOffset;
    }

    public static void setTimingOffset(double timingOffset) {
        GameHelper.timingOffset = timingOffset;
    }

    public static int getTimeSignature() {
        return timeSignature;
    }

    public static void setTimeSignature(int timeSignature) {
        GameHelper.timeSignature = timeSignature;
    }

    public static double getInitalBeatLength() {
        return initalBeatLength;
    }

    public static void setInitalBeatLength(double initalBeatLength) {
        GameHelper.initalBeatLength = initalBeatLength;
    }

    public static double getSliderTickLength() {
        return 100f * initalBeatLength / speed;
    }

    public static double getKiaiTickLength() {
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
            float f1;
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

        public ArrayList<PointF> points = new ArrayList<>();

        public ArrayList<Float> length = new ArrayList<>();

    }

}
