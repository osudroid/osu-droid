package ru.nsu.ccfit.zuev.osu.game;

import com.edlplan.framework.math.Vec2;
import com.edlplan.framework.math.line.LinePath;
import com.rian.osu.beatmap.hitobject.Slider;
import com.rian.osu.beatmap.hitobject.SliderPathType;
import com.rian.osu.mods.*;
import com.rian.osu.utils.PathApproximation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ru.nsu.ccfit.zuev.osu.Constants;
import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameHelper {
    private static float overallDifficulty = 1;
    private static float originalTimePreempt = 0;
    private static float healthDrain = 0;
    private static float speedMultiplier = 0;
    private static ModHidden hidden;
    private static ModTraceable traceable;
    private static ModFlashlight flashlight;
    private static ModHardRock hardRock;
    private static ModRelax relax;
    private static ModDoubleTime doubleTime;
    private static ModNightCore nightCore;
    private static ModHalfTime halfTime;
    private static ModAutopilot autopilot;
    private static ModSuddenDeath suddenDeath;
    private static ModPerfect perfect;
    private static ModSynesthesia synesthesia;
    private static ModScoreV2 scoreV2;
    private static ModEasy easy;
    private static ModMuted muted;
    private static ModFreezeFrame freezeFrame;
    private static ModApproachDifferent approachDifferent;
    private static boolean isKiai = false;
    private static ModAutoplay autoplay;
    private static ModPrecise precise;
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

    public static float getOriginalTimePreempt() {
        return originalTimePreempt;
    }

    public static void setOriginalTimePreempt(final float originalTimePreempt) {
        GameHelper.originalTimePreempt = originalTimePreempt;
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

    public static ModEasy getEasy() {
        return easy;
    }

    public static boolean isEasy() {
        return easy != null;
    }

    public static void setEasy(final ModEasy isEasy) {
        GameHelper.easy = isEasy;
    }

    public static ModHardRock getHardRock() {
        return hardRock;
    }

    public static boolean isHardRock() {
        return hardRock != null;
    }

    public static void setHardRock(final ModHardRock hardRock) {
        GameHelper.hardRock = hardRock;
    }

    public static ModHidden getHidden() {
        return hidden;
    }

    public static boolean isHidden() {
        return hidden != null;
    }

    public static void setHidden(final ModHidden hidden) {
        GameHelper.hidden = hidden;
    }

    public static ModTraceable getTraceable() {
        return traceable;
    }

    public static boolean isTraceable() {
        return traceable != null;
    }

    public static void setTraceable(final ModTraceable traceable) {
        GameHelper.traceable = traceable;
    }

    public static ModFlashlight getFlashlight() {
        return flashlight;
    }

    public static boolean isFlashlight() {
        return flashlight != null;
    }

    public static void setFlashlight(final ModFlashlight flashlight) {
        GameHelper.flashlight = flashlight;
    }

    public static ModHalfTime getHalfTime() {
        return halfTime;
    }

    public static boolean isHalfTime() {
        return halfTime != null;
    }

    public static void setHalfTime(final ModHalfTime halfTime) {
        GameHelper.halfTime = halfTime;
    }

    public static ModNightCore getNightCore() {
        return nightCore;
    }

    public static boolean isNightCore() {
        return nightCore != null;
    }

    public static void setNightCore(final ModNightCore nightCore) {
        GameHelper.nightCore = nightCore;
    }

    public static ModDoubleTime getDoubleTime() {
        return doubleTime;
    }

    public static boolean isDoubleTime() {
        return doubleTime != null;
    }

    public static void setDoubleTime(final ModDoubleTime doubleTime) {
        GameHelper.doubleTime = doubleTime;
    }

    public static ModSuddenDeath getSuddenDeath() {
        return suddenDeath;
    }

    public static boolean isSuddenDeath() {
        return suddenDeath != null;
    }

    public static void setSuddenDeath(final ModSuddenDeath suddenDeath) {
        GameHelper.suddenDeath = suddenDeath;
    }

    public static ModPerfect getPerfect() {
        return perfect;
    }

    public static boolean isPerfect() {
        return perfect != null;
    }

    public static void setPerfect(final ModPerfect perfect) {
        GameHelper.perfect = perfect;
    }

    public static ModSynesthesia getSynesthesia() {
        return synesthesia;
    }

    public static boolean isSynesthesia() {
        return synesthesia != null;
    }

    public static void setSynesthesia(final ModSynesthesia synesthesia) {
        GameHelper.synesthesia = synesthesia;
    }

    public static ModMuted getMuted() {
        return muted;
    }

    public static boolean isMuted() {
        return muted != null;
    }

    public static void setMuted(final ModMuted muted) {
        GameHelper.muted = muted;
    }

    public static ModFreezeFrame getFreezeFrame() {
        return freezeFrame;
    }

    public static boolean isFreezeFrame() {
        return freezeFrame != null;
    }

    public static void setFreezeFrame(final ModFreezeFrame freezeFrame) {
        GameHelper.freezeFrame = freezeFrame;
    }

    public static ModApproachDifferent getApproachDifferent() {
        return approachDifferent;
    }

    public static boolean isApproachDifferent() {
        return approachDifferent != null;
    }

    public static void setApproachDifferent(final ModApproachDifferent approachDifferent) {
        GameHelper.approachDifferent = approachDifferent;
    }

    public static ModScoreV2 getScoreV2() {
        return scoreV2;
    }

    public static boolean isScoreV2() {
        return scoreV2 != null;
    }

    public static void setScoreV2(final ModScoreV2 scoreV2) {
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

    public static ModRelax getRelax() {
        return relax;
    }

    public static boolean isRelax() {
        return relax != null;
    }

    public static void setRelax(final ModRelax relax) {
        GameHelper.relax = relax;
    }

    public static ModAutopilot getAutopilot() {
        return autopilot;
    }

    public static boolean isAutopilot() {
        return autopilot != null;
    }

    public static void setAutopilot(final ModAutopilot autopilot) {
        GameHelper.autopilot = autopilot;
    }

    public static ModAutoplay getAutoplay() {
        return autoplay;
    }

    public static boolean isAutoplay() {
        return autoplay != null;
    }

    public static void setAutoplay(final ModAutoplay autoplay) {
        GameHelper.autoplay = autoplay;
    }

    public static ModPrecise getPrecise() {
        return precise;
    }

    public static boolean isPrecise() {
        return precise != null;
    }

    public static void setPrecise(final ModPrecise precise) {
        GameHelper.precise = precise;
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
