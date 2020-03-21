package lt.ekgame.beatmap_analyzer.performance;

import lt.ekgame.beatmap_analyzer.difficulty.Difficulty;
import lt.ekgame.beatmap_analyzer.performance.scores.Score;

public interface PerformanceCalculator {

    public Performance calculate(Difficulty difficulty, Score score);

}
