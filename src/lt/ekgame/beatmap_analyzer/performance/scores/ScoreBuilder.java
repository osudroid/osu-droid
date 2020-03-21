package lt.ekgame.beatmap_analyzer.performance.scores;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.utils.MathUtils;
import lt.ekgame.beatmap_analyzer.utils.ScoreVersion;

public class ScoreBuilder {

    private Beatmap beatmap;
    private int score = 1000000, combo, numMiss = 0;
    private double accuracy = 1;
    private ScoreVersion version = ScoreVersion.V2;

    public ScoreBuilder(Beatmap beatmap) {
        this.beatmap = beatmap;
        this.combo = beatmap.getMaxCombo();
    }

    public ScoreBuilder version(ScoreVersion version) {
        this.version = version;
        return this;
    }

    public ScoreBuilder score(int score) {
        this.score = score;
        return this;
    }

    public ScoreBuilder combo(int combo) {
        this.combo = combo;
        return this;
    }

    public ScoreBuilder accuracy(double accuracy) {
        return accuracy(accuracy, 0);
    }

    public ScoreBuilder accuracy(double accuracy, int numMiss) {
        this.numMiss = numMiss;
        this.accuracy = accuracy;
        return this;
    }

    public ScoreBuilder osuAccuracy(int num100, int num50) {
        return osuAccuracy(num100, num50, 0);
    }

    public ScoreBuilder osuAccuracy(int num100, int num50, int numMiss) {
        int num300 = beatmap.getObjectCount() - num100 - num50 - numMiss;
        return accuracy(MathUtils.calculateOsuAccuracy(num300, num100, num50, numMiss), numMiss);
    }

    public ScoreBuilder taikoAccuracy(int numHalf) {
        return taikoAccuracy(numHalf, 0);
    }

    public ScoreBuilder taikoAccuracy(int numHalf, int numMiss) {
        int numGreat = beatmap.getMaxCombo() - numHalf - numMiss;
        return accuracy(MathUtils.calculateTaikoAccuracy(numGreat, numHalf, numMiss), numMiss);
    }

    public ScoreBuilder maniaAccuracy(int num200, int num100) {
        return maniaAccuracy(num200, num100, 0, 0);
    }

    public ScoreBuilder maniaAccuracy(int num200, int num100, int num50) {
        return maniaAccuracy(num200, num100, num50, 0);
    }

    public ScoreBuilder maniaAccuracy(int num200, int num100, int num50, int numMiss) {
        int num300 = beatmap.getObjectCount() - num200 - num100 - num50 - numMiss;
        return accuracy(MathUtils.calculateManiaAccuracy(0, num300, num200, num100, num50, numMiss), numMiss);
    }

    public ScoreBuilder catchAccuracy(int missedDroplets, int numMiss) {
        int hits = beatmap.getMaxCombo() - missedDroplets - numMiss;
        return accuracy(MathUtils.calculateCatchAccuracy(hits, missedDroplets + numMiss), numMiss);
    }

    public Score build() {
        return new Score(score, combo, accuracy, numMiss, version);
    }
}
