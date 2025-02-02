package ru.nsu.ccfit.zuev.osu.game;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.SliderPathType;
import com.rian.osu.utils.PathApproximation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

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
    private static int replayVersion;

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
     * Converts a {@link SliderPath} to a {@link LinePath} that can be used to render the slider path.
     *
     * @param sliderPath The {@link SliderPath} to convert.
     * @return The converted {@link LinePath}.
     */
    public static LinePath convertSliderPath(final SliderPath sliderPath) {
        var renderPath = new LinePath();

        if (sliderPath.anchorCount == 0) {
            return renderPath;
        }

        // osu!stable optimizes gameplay path rendering by only including points that are 6 osu!pixels apart.
        // In linear paths, the distance threshold is further extended to 32 osu!pixels.
        int distanceThreshold = sliderPath.pathType == SliderPathType.Linear ? 32 : 6;

        // Invert the scale to convert from osu!pixels to screen pixels.
        var invertedScale = new Vec2(
        (float) Constants.MAP_WIDTH / Constants.MAP_ACTUAL_WIDTH,
        (float) Constants.MAP_HEIGHT / Constants.MAP_ACTUAL_HEIGHT
        );

        // Additional consideration for Catmull sliders that form "bulbs" around points with identical positions.
        boolean isCatmull = sliderPath.pathType == SliderPathType.Catmull;
        int catmullSegmentLength = PathApproximation.CATMULL_DETAIL * 2;

        Vec2 lastStart = null;

        for (int i = 0; i < sliderPath.anchorCount; ++i) {
            var x = sliderPath.getX(i);
            var y = sliderPath.getY(i);
            var vec = new Vec2(x, y);

            if (lastStart == null) {
                renderPath.add(vec);
                lastStart = vec;
                continue;
            }

            float distanceFromStart = vec.copy().minus(lastStart).multiple(invertedScale).length();

            if (distanceFromStart > distanceThreshold || i == sliderPath.anchorCount - 1 ||
                    (isCatmull && (i + 1) % catmullSegmentLength == 0)) {
                renderPath.add(vec);
                lastStart = null;
            }
        }

        // The render path may under-measure the true length of the slider due to the optimization.
        // Normally, the path would need to be extended to account for the true length of the slider.
        // However, in this case we simply let it be as the path is still rendered correctly (its points
        // are still in the correct positions).
        renderPath.measure();

        return renderPath;
    }

    /**
     * Converts an osu!pixels-based path of a {@link Slider} to one that can be used in gameplay.
     *
     * @return The converted {@link SliderPath}.
     */
    public static SliderPath convertSliderPath(final Slider slider) {
        var calculatedPath = slider.getPath().getCalculatedPath();
        var cumulativeLength = slider.getPath().getCumulativeLength();
        var path = new SliderPath(slider.getPath().getPathType(), calculatedPath.size());

        float widthScale = (float) Constants.MAP_ACTUAL_WIDTH / Constants.MAP_WIDTH;
        float heightScale = (float) Constants.MAP_ACTUAL_HEIGHT / Constants.MAP_HEIGHT;

        for (int i = 0; i < calculatedPath.size(); i++) {
            var p = calculatedPath.get(i);

            path.set(i, p.x * widthScale, p.y * heightScale, cumulativeLength.get(i).floatValue());
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

    public static int getReplayVersion() {
        return replayVersion;
    }

    public static void setReplayVersion(int replayVersion) {
        GameHelper.replayVersion = replayVersion;
    }

    public static class SliderPath {

        private static final int strip = 3;
        private static final int offsetX = 0;
        private static final int offsetY = 1;
        private static final int offsetLength = 2;

        private final float[] data;

        public final SliderPathType pathType;
        public int anchorCount = 0;

        public SliderPath(SliderPathType pathType, int anchorPointCount) {
            this.pathType = pathType;
            data = new float[anchorPointCount * strip];
        }

        public void set(int index, float x, float y, float length) {
            data[index * strip + offsetX] = x;
            data[index * strip + offsetY] = y;
            data[index * strip + offsetLength] = length;
            anchorCount++;
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
