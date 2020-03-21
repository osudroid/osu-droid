package lt.ekgame.beatmap_analyzer.beatmap;

import lt.ekgame.beatmap_analyzer.parser.BeatmapException;
import lt.ekgame.beatmap_analyzer.parser.FilePart;
import lt.ekgame.beatmap_analyzer.parser.FilePartConfig;

public class BeatmapDifficulties {

    private double healthDrain;
    private double cirlceSize;
    private double overallDifficulty;
    private double approachRate;
    private double sliderMultiplier;
    private double tickRate;

    private BeatmapDifficulties() {
    }

    public BeatmapDifficulties(FilePart part) throws BeatmapException {
        FilePartConfig config = new FilePartConfig(part);
        healthDrain = config.getDouble("HPDrainRate");
        cirlceSize = config.getDouble("CircleSize");
        overallDifficulty = config.getDouble("OverallDifficulty");
        // Long ago, osu! OD and AR used to be the same setting, so some maps don't have it.
        approachRate = config.getDouble("ApproachRate", overallDifficulty);
        sliderMultiplier = config.getDouble("SliderMultiplier");
        tickRate = config.getDouble("SliderTickRate");
    }

    public BeatmapDifficulties clone() {
        BeatmapDifficulties clone = new BeatmapDifficulties();
        clone.healthDrain = this.healthDrain;
        clone.cirlceSize = this.cirlceSize;
        clone.overallDifficulty = this.overallDifficulty;
        clone.approachRate = this.approachRate;
        clone.sliderMultiplier = this.sliderMultiplier;
        clone.tickRate = this.tickRate;
        return clone;
    }

    public double getHP() {
        return healthDrain;
    }

    public void setHP(double healthDrain) {
        this.healthDrain = healthDrain;
    }

    public double getCS() {
        return cirlceSize;
    }

    public void setCS(double cirlceSize) {
        this.cirlceSize = cirlceSize;
    }

    public double getOD() {
        return overallDifficulty;
    }

    public void setOD(double overallDifficulty) {
        this.overallDifficulty = overallDifficulty;
    }

    public double getAR() {
        return approachRate;
    }

    public void setAR(double approachRate) {
        this.approachRate = approachRate;
    }

    public double getSliderMultiplier() {
        return sliderMultiplier;
    }

    public void setSliderMultiplier(double sliderMultiplier) {
        this.sliderMultiplier = sliderMultiplier;
    }

    public double getTickRate() {
        return tickRate;
    }

    public void setTickRate(double tickRate) {
        this.tickRate = tickRate;
    }
}
