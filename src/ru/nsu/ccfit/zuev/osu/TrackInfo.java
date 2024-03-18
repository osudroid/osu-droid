package ru.nsu.ccfit.zuev.osu;

import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.io.File;
import java.io.Serializable;

public class TrackInfo implements Serializable {
    private static final long serialVersionUID = 2049627581836712912L;

    private String filename;

    private String publicName;
    private String mode;
    private String creator;
    private String md5;
    private String background = null;
    private int beatmapID = 0;
    private int beatmapSetID = 0;
    private float droidDifficulty;
    private float standardDifficulty;
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


    /**The audio path relative to beatmap folder.*/
    private String audioFilename;

    /**The audio preview time.*/
    private int previewTime = -1;


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

    public float getStandardDifficulty() {
        return standardDifficulty;
    }

    public void setStandardDifficulty(final float standardDifficulty) {
        this.standardDifficulty = standardDifficulty;
    }

    public float getDroidDifficulty() {
        return droidDifficulty;
    }

    public void setDroidDifficulty(float droidDifficulty) {
        this.droidDifficulty = droidDifficulty;
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

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    public String getMD5() {
        return md5;
    }

    // Sometimes when the library is reloaded there can be 2 instances for the same beatmap so checking its MD5 is the
    // proper way to compare
    @Override
    public boolean equals(Object o) {

        if (o == this)
            return true;

        if (o instanceof TrackInfo) {
            var track = (TrackInfo) o;

            return md5 != null
                    && track.md5 != null
                    && track.md5.equals(md5);
        }
        return false;
    }

    public String getAudioFilename() {
        return audioFilename;
    }

    public void setAudioFilename(String audioFilename) {
        this.audioFilename = audioFilename;
    }

    public int getPreviewTime() {
        return previewTime;
    }

    public void setPreviewTime(int previewTime) {
        this.previewTime = previewTime;
    }

    public boolean populate(Beatmap beatmap) {
        md5 = beatmap.md5;
        creator = beatmap.metadata.creator;
        mode = beatmap.metadata.version;
        publicName = beatmap.metadata.artist + " - " + beatmap.metadata.title;
        beatmapID = beatmap.metadata.beatmapId;
        beatmapSetID = beatmap.metadata.beatmapSetId;

        // General
        var musicFile = new File(beatmap.folder, beatmap.general.audioFilename);
        if (!musicFile.exists()) {
            ToastLogger.showText(StringTable.format(R.string.beatmap_parser_music_not_found,
                    filename.substring(0, Math.max(0, filename.length() - 4))), true);
            return false;
        }

        audioFilename = musicFile.getPath();
        previewTime = beatmap.general.previewTime;

        // Difficulty
        overallDifficulty = beatmap.difficulty.od;
        approachRate = beatmap.difficulty.getAr();
        hpDrain = beatmap.difficulty.hp;
        circleSize = beatmap.difficulty.cs;

        // Events
        background = beatmap.folder + "/" + beatmap.events.backgroundFilename;

        // Timing points
        for (var point : beatmap.controlPoints.timing.getControlPoints()) {
            float bpm = (float) point.getBpm();

            bpmMin = bpmMin != Float.MAX_VALUE ? Math.min(bpmMin, bpm) : bpm;
            bpmMax = bpmMax != 0 ? Math.max(bpmMax, bpm) : bpm;
        }

        // Hit objects
        if (beatmap.hitObjects.objects.isEmpty()) {
            return false;
        }

        setTotalHitObjectCount(beatmap.hitObjects.objects.size());
        setHitCircleCount(beatmap.hitObjects.getCircleCount());
        setSliderCount(beatmap.hitObjects.getSliderCount());
        setSpinnerCount(beatmap.hitObjects.getSpinnerCount());
        setMusicLength(beatmap.getDuration());
        setMaxCombo(beatmap.getMaxCombo());

        var droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmap);
        var standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmap);

        setDroidDifficulty(GameHelper.Round(droidAttributes.starRating, 2));
        setStandardDifficulty(GameHelper.Round(standardAttributes.starRating, 2));

        return true;
    }
}
