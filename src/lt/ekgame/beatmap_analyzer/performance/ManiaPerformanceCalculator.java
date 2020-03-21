package lt.ekgame.beatmap_analyzer.performance;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.difficulty.Difficulty;
import lt.ekgame.beatmap_analyzer.difficulty.ManiaDifficulty;
import lt.ekgame.beatmap_analyzer.performance.scores.Score;
import lt.ekgame.beatmap_analyzer.utils.Mod;

public class ManiaPerformanceCalculator implements PerformanceCalculator {

    @Override
    public Performance calculate(Difficulty difficulty, Score score) {
        ManiaDifficulty diff = (ManiaDifficulty) difficulty;

        double strainValue = calculateStrain(diff, score);
        double accValue = calculateAccuracy(diff, score);

        double multiplier = 1.1;
        if (diff.hasMod(Mod.NO_FAIL)) multiplier *= 0.9;
        if (diff.hasMod(Mod.SPUN_OUT)) multiplier *= 0.95;
        if (diff.hasMod(Mod.EASY)) multiplier *= 0.5;

        double performance = Math.pow(Math.pow(strainValue, 1.1) + Math.pow(accValue, 1.1), 1 / 1.1) * multiplier;
        return new Performance(score.getAccuracy(), performance, 0, strainValue, accValue);
    }

    private double calculateStrain(ManiaDifficulty difficulty, Score score) {
        double scoreMultiplier = difficulty.getMods().getScoreMultiplier(Gamemode.MANIA);
        if (scoreMultiplier <= 0)
            return 0;

        int actualScore = (int) Math.round(score.getScore() * (1 / scoreMultiplier));

        double strainValue = Math.pow(5 * Math.max(1, difficulty.getStars() / 0.0825f) - 4, 3) / 110000;
        strainValue *= 1 + 0.1 * Math.min(1, difficulty.getObjectCount() / 1500.0);

        if (actualScore <= 500_000)
            strainValue *= actualScore / 500_000.0 * 0.1;
        else if (actualScore <= 600_000)
            strainValue *= 0.1 + (actualScore - 500_000) / 100_000.0 * 0.2;
        else if (actualScore <= 700_000)
            strainValue *= 0.3 + (actualScore - 600_000) / 100_000.0 * 0.35;
        else if (actualScore <= 800_000)
            strainValue *= 0.65 + (actualScore - 700_000) / 100_000.0 * 0.2;
        else if (actualScore <= 900_000)
            strainValue *= 0.85 + (actualScore - 800_000) / 100_000.0 * 0.1;
        else
            strainValue *= 0.95 + (actualScore - 900_000) / 100_000.0 * 0.05;

        return strainValue;
    }

    private double calculateAccuracy(ManiaDifficulty difficulty, Score score) {
        double od = Math.min(10, Math.max(0, 10 - difficulty.getOD()));
        double hitWindow = (34 + 3 * od);///difficulty.getSpeedMultiplier();
        if (hitWindow <= 0)
            return 0;

        double accuracyValue = Math.pow(150 / hitWindow * Math.pow(score.getAccuracy(), 16), 1.8) * 2.5;
        accuracyValue *= Math.min(1.15, Math.pow(difficulty.getObjectCount() / 1500.0, 0.3));
        return accuracyValue;
    }
}
