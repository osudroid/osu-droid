package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;

public class GameHelper {
    private static double speed = 1;
    private static float difficulty = 1;
    private static float approachRate = 1;
    private static float drain = 0;
    private static float speedMultiplier = 0;
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
    private static boolean isKiai = false;
    private static boolean auto = false;
    private static double beatLength = 0;
    private static double timingOffset = 0;
    private static int timeSignature = 4;
    private static double initalBeatLength = 0;
    private static double globalTime = 0;
    private static final Queue<SliderPath> pathPool = new LinkedList<>();
    private static final Queue<PointF> pointPool = new LinkedList<>();

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

    /**
     * Converts a difficulty-calculated slider path of a {@link com.rian.osu.beatmap.hitobject.Slider} to one that can be used in gameplay.
     *
     * @return The converted {@link SliderPath}.
     */
    public static SliderPath convertSliderPath(final com.rian.osu.beatmap.hitobject.Slider slider) {
        var path = newPath();
        var startPosition = slider.position.plus(slider.getGameplayStackOffset());

        for (var p : slider.getPath().getCalculatedPath()) {
            var point = newPointF();
            point.set(startPosition.x + p.x, startPosition.y + p.y);

            path.points.add(Utils.trackToRealCoords(point));
        }

        for (double l : slider.getPath().getCumulativeLength()) {
            path.length.add((float) l);
        }

        return path;
    }

    public static float getApproachRate() {
        return approachRate;
    }

    public static void setApproachRate(float approachRate) {
        GameHelper.approachRate = approachRate;
    }

    /**
     * Gets the rate at which gameplay progresses in terms of time.
     */
    public static float getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Sets the rate at which gameplay progresses in terms of time.
     */
    public static void setSpeedMultiplier(float speedMultiplier) {
        GameHelper.speedMultiplier = speedMultiplier;
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

    public static void setInitalBeatLength(double initalBeatLength) {
        GameHelper.initalBeatLength = initalBeatLength;
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
        public ArrayList<PointF> points = new ArrayList<>();
        public ArrayList<Float> length = new ArrayList<>();
    }
}
