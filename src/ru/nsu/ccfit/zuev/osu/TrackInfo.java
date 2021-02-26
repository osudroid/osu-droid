package ru.nsu.ccfit.zuev.osu;

import java.io.Serializable;

public class TrackInfo implements Serializable {
    private static final long serialVersionUID = 2049627581836712912L;

    private String filename;

    private String publicName;
    private String mode;
    private String creator;
    private String background = null;
    private int beatmapID = 0;
    private int beatmapSetID = 0;
    private float difficulty;
    private float hpDrain;
    private float overallDifficulty;
    private float approachRate;
    private float circleSize;
    private float bpmMax = 0;
    private float bpmMin = Float.MAX_VALUE;
    private long musicLength = 0;
    private int hitCircleCount = 0;
    private int sliderCount = 0;
    private int spinnerCount = 0;
    private int totalHitObjectCount = 0;
    private int maxCombo = 0;

    private BeatmapInfo beatmap;

    public TrackInfo(BeatmapInfo beatmap) {
        this.beatmap = beatmap;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(final float difficulty) {
        this.difficulty = difficulty;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(final String background) {
        this.background = background;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(final String publicName) {
        this.publicName = publicName;
    }

    public BeatmapInfo getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(BeatmapInfo beatmap) {
        this.beatmap = beatmap;
    }

    public float getHpDrain() {
        return hpDrain;
    }

    public void setHpDrain(float hpDrain) {
        this.hpDrain = hpDrain;
    }

    public float getOverallDifficulty() {
        return overallDifficulty;
    }

    public void setOverallDifficulty(float overallDifficulty) {
        this.overallDifficulty = overallDifficulty;
    }

    public float getApproachRate() {
        return approachRate;
    }

    public void setApproachRate(float approachRate) {
        this.approachRate = approachRate;
    }

    public float getCircleSize() {
        return circleSize;
    }

    public void setCircleSize(float circleSize) {
        this.circleSize = circleSize;
    }

    public float getBpmMax() {
        return bpmMax;
    }

    public void setBpmMax(float bpmMax) {
        this.bpmMax = bpmMax;
    }

    public float getBpmMin() {
        return bpmMin;
    }

    public void setBpmMin(float bpmMin) {
        this.bpmMin = bpmMin;
    }

    public long getMusicLength() {
        return musicLength;
    }

    public void setMusicLength(long musicLength) {
        this.musicLength = musicLength;
    }

    public int getHitCircleCount() {
        return hitCircleCount;
    }

    public void setHitCircleCount(int hitCircleCount) {
        this.hitCircleCount = hitCircleCount;
    }

    public int getSliderCount() {
        return sliderCount;
    }

    public void setSliderCount(int sliderCount) {
        this.sliderCount = sliderCount;
    }

    public int getSpinnerCount() {
        return spinnerCount;
    }

    public void setSpinnerCount(int spinnerCount) {
        this.spinnerCount = spinnerCount;
    }

    public int getTotalHitObjectCount() {
        return totalHitObjectCount;
    }

    public void setTotalHitObjectCount(int totalHitObjectCount) {
        this.totalHitObjectCount = totalHitObjectCount;
    }

    public int getBeatmapID() {
        return beatmapID;
    }

    public void setBeatmapID(int beatmapID) {
        this.beatmapID = beatmapID;
    }

    public int getBeatmapSetID() {
        return beatmapSetID;
    }

    public void setBeatmapSetID(int beatmapSetID) {
        this.beatmapSetID = beatmapSetID;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(int maxCombo) {
        this.maxCombo = maxCombo;
    }
}
