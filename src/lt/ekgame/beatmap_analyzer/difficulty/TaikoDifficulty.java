package lt.ekgame.beatmap_analyzer.difficulty;

import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoBeatmap;
import lt.ekgame.beatmap_analyzer.performance.Performance;
import lt.ekgame.beatmap_analyzer.performance.TaikoPerformanceCalculator;
import lt.ekgame.beatmap_analyzer.performance.scores.Score;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class TaikoDifficulty extends Difficulty {

    public TaikoDifficulty(TaikoBeatmap beatmap, Mods mods, double starDiff, List<Double> strains) {
        super(beatmap, mods, starDiff, strains);
    }

    @Override
    public Performance getPerformance(Score score) {
        return new TaikoPerformanceCalculator().calculate(this, score);
    }
}
