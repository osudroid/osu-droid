package lt.ekgame.beatmap_analyzer.difficulty;

import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public interface DifficultyCalculator {

    public Difficulty calculate(Mods mods, Beatmap beatmap);

    public double calculateDifficulty(List<Double> strains);

}
