package lt.ekgame.beatmap_analyzer.beatmap;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.beatmap_analyzer.parser.FilePart;
import lt.ekgame.beatmap_analyzer.parser.FilePartConfig;

public class BeatmapGenerals {

    private String audioFileName;
    private int audioLeadIn;
    private int previewTime;
    private boolean hasCountdown;
    private String sampleSet;
    private double stackLeniency;
    private Gamemode gamemode;
    private boolean hasLetterboxing;
    private boolean hasEpilepsyWarning;
    private boolean hasWidescreenStoryboard;

    private BeatmapGenerals() {
    }

    ;

    public BeatmapGenerals(FilePart part) throws BeatmapException {
        FilePartConfig config = new FilePartConfig(part);
        audioFileName = config.getString("AudioFilename");
        audioLeadIn = config.getInt("AudioLeadIn", 0);
        previewTime = config.getInt("PreviewTime");
        hasCountdown = config.getBoolean("Countdown", true);
        sampleSet = config.getString("SampleSet");
        stackLeniency = config.getDouble("StackLeniency", 0.7);
        gamemode = config.getGamemode("Mode", Gamemode.OSU);
        hasLetterboxing = config.getBoolean("LetterboxInBreaks", true);
        hasEpilepsyWarning = config.getBoolean("EpilepsyWarning", false);
        hasWidescreenStoryboard = config.getBoolean("WidescreenStoryboard", false);
    }

    public BeatmapGenerals clone() {
        BeatmapGenerals clone = new BeatmapGenerals();
        clone.audioFileName = this.audioFileName;
        clone.audioLeadIn = this.audioLeadIn;
        clone.previewTime = this.previewTime;
        clone.hasCountdown = this.hasCountdown;
        clone.sampleSet = this.sampleSet;
        clone.stackLeniency = this.stackLeniency;
        clone.gamemode = this.gamemode;
        clone.hasLetterboxing = this.hasLetterboxing;
        clone.hasEpilepsyWarning = this.hasEpilepsyWarning;
        clone.hasWidescreenStoryboard = this.hasWidescreenStoryboard;
        return clone;
    }

    public String getAudioFileName() {
        return audioFileName;
    }

    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public int getAudioLeadIn() {
        return audioLeadIn;
    }

    public void setAudioLeadIn(int audioLeadIn) {
        this.audioLeadIn = audioLeadIn;
    }

    public int getPreviewTime() {
        return previewTime;
    }

    public void setPreviewTime(int previewTime) {
        this.previewTime = previewTime;
    }

    public boolean hasCountdown() {
        return hasCountdown;
    }

    public String getSampleSet() {
        return sampleSet;
    }

    public void setSampleSet(String sampleSet) {
        this.sampleSet = sampleSet;
    }

    public double getStackLeniency() {
        return stackLeniency;
    }

    public void setStackLeniency(double stackLeniency) {
        this.stackLeniency = stackLeniency;
    }

    public Gamemode getGamemode() {
        return gamemode;
    }

    public void setGamemode(Gamemode gamemode) {
        this.gamemode = gamemode;
    }

    public boolean hasLetterboxing() {
        return hasLetterboxing;
    }

    public boolean hasEpilepsyWarning() {
        return hasEpilepsyWarning;
    }

    public boolean hasWidescreenStoryboard() {
        return hasWidescreenStoryboard;
    }

    public void setHasCountdown(boolean hasCountdown) {
        this.hasCountdown = hasCountdown;
    }

    public void setLetterboxing(boolean hasLetterboxing) {
        this.hasLetterboxing = hasLetterboxing;
    }

    public void setEpilepsyWarning(boolean hasEpilepsyWarning) {
        this.hasEpilepsyWarning = hasEpilepsyWarning;
    }

    public void setWidescreenStoryboard(boolean hasWidescreenStoryboard) {
        this.hasWidescreenStoryboard = hasWidescreenStoryboard;
    }
}
