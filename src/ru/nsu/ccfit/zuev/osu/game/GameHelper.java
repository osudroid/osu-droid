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
    private static float overallDifficulty = 1;
    private static float objectTimePreempt = 1;
    private static float healthDrain = 0;
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
    private static double currentBeatTime = 0;
    private static final Queue<SliderPath> pathPool = new LinkedList<>();
    private static final Queue<PointF> pointPool = new LinkedList<>();

    private static DifficultyHelper difficultyHelper = DifficultyHelper.StdDifficulty;

    public static DifficultyHelper getDifficultyHelper() {
        return difficultyHelper;
    }

    public static void setDifficultyHelper(DifficultyHelper difficultyHelper) {
        GameHelper.difficultyHelper = difficultyHelper;
    }

    public static float getHealthDrain() {
        return healthDrain;
    }

    public static void setHealthDrain(final float healthDrain) {
        GameHelper.healthDrain = healthDrain;
    }

    public static float getOverallDifficulty() {
        return overallDifficulty;
    }

    public static void setOverallDifficulty(final float overallDifficulty) {
        GameHelper.overallDifficulty = overallDifficulty;
    }

    /**
     * Converts a difficulty-calculated slider path of a {@link com.rian.osu.beatmap.hitobject.Slider} to one that can be used in gameplay.
     *
     * @return The converted {@link SliderPath}.
     */
    public static SliderPath convertSliderPath(final com.rian.osu.beatmap.hitobject.Slider slider) {
        var path = newPath();
        var startPosition = slider.getPosition().plus(slider.getGameplayStackOffset());

        for (var p : slider.getPath().getCalculatedPath()) {
            var point = newPointF();
            point.set(startPosition.x + p.x, startPosition.y + p.y);

            // The path is already flipped when the library applies the Hard Rock mod, so we don't need to do it here.
            path.points.add(Utils.trackToRealCoords(point));
        }

        for (double l : slider.getPath().getCumulativeLength()) {
            path.length.add((float) l);
        }

        return path;
    }

    public static float getObjectTimePreempt() {
        return objectTimePreempt;
    }

    public static void setObjectTimePreempt(float objectTimePreempt) {
        GameHelper.objectTimePreempt = objectTimePreempt;
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

    public static double getCurrentBeatTime() {
        return currentBeatTime;
    }

    public static void setCurrentBeatTime(final double currentBeatTime) {
        GameHelper.currentBeatTime = currentBeatTime;
    }

    public static double getBeatLength() {
        return beatLength;
    }

    public static void setBeatLength(final double beatLength) {
        GameHelper.beatLength = beatLength;
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
