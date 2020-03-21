package lt.ekgame.beatmap_analyzer.difficulty;

import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.mania.ManiaBeatmap;
import lt.ekgame.beatmap_analyzer.performance.ManiaPerformanceCalculator;
import lt.ekgame.beatmap_analyzer.performance.Performance;
import lt.ekgame.beatmap_analyzer.performance.scores.Score;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class ManiaDifficulty extends Difficulty {

    public ManiaDifficulty(ManiaBeatmap beatmap, Mods mods, double starDiff, List<Double> strains) {
        super(beatmap, mods, starDiff, strains);
    }

    @Override
    public Performance getPerformance(Score score) {
        return new ManiaPerformanceCalculator().calculate(this, score);
    }
}
