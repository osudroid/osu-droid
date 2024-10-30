package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.rian.osu.beatmap.hitobject.Slider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyHelper;

public class GameHelper {
    private static float overallDifficulty = 1;
    private static float healthDrain = 0;
    private static float speedMultiplier = 0;
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
    private static boolean samplesMatchPlaybackRate;

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
     * Converts an osu!pixels-based path of a {@link Slider} to one that can be used in gameplay.
     *
     * @return The converted {@link SliderPath}.
     */
    public static SliderPath convertSliderPath(final Slider slider) {
        var startPosition = slider.getPosition().plus(slider.getGameplayStackOffset());

        var calculatedPath = slider.getPath().getCalculatedPath();
        var cumulativeLength = slider.getPath().getCumulativeLength();

        var path = new SliderPath(calculatedPath.size());
        var tmpPoint = new PointF();

        for (var i = 0; i < calculatedPath.size(); i++) {

            var p = calculatedPath.get(i);
            tmpPoint.set(startPosition.x + p.x, startPosition.y + p.y);

            // The path is already flipped when the library applies the Hard Rock mod, so we don't need to do it here.
            Utils.trackToRealCoords(tmpPoint);
            path.setPoint(i, tmpPoint.x, tmpPoint.y);

            if (i < cumulativeLength.size()) {
                path.setLength(i, cumulativeLength.get(i).floatValue());
            } else {
                path.setLength(i, -1f);
            }
        }

        return path;
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

    public static boolean isSamplesMatchPlaybackRate() {
        return samplesMatchPlaybackRate;
    }

    public static void setSamplesMatchPlaybackRate(boolean samplesMatchPlaybackRate) {
        GameHelper.samplesMatchPlaybackRate = samplesMatchPlaybackRate;
    }

    public static class SliderPath {

        private static final int strip = 3;
        private static final int offsetX = 0;
        private static final int offsetY = 1;
        private static final int offsetLength = 2;

        private float[] data;

        public int lengthCount = 0;
        public int pointCount = 0;

        public SliderPath(int anchorPointCount) {
            data = new float[anchorPointCount * strip];
        }

        public void setPoint(int index, float x, float y) {
            data[index * strip + offsetX] = x;
            data[index * strip + offsetY] = y;
            pointCount++;
        }

        public void setLength(int index, float length) {

            var targetIndex = index * strip + offsetLength;

            // This condition can be triggered if there's a mismatch between the number of points
            // and the number of lengths. Should never happen in practice, but it's better to be safe.
            if (targetIndex >= data.length) {
                data = Arrays.copyOf(data, data.length + strip);
            }

            data[targetIndex] = length;
            lengthCount++;
        }


        public float getX(int index) {
            return data[index * strip + offsetX];
        }

        public float getY(int index) {
            return data[index * strip + offsetY];
        }

        public float getLength(int index) {
            return data[index * strip + offsetLength];
        }

    }
}
