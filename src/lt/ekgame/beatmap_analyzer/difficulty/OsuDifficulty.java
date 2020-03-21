package lt.ekgame.beatmap_analyzer.difficulty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuCircle;
import lt.ekgame.beatmap_analyzer.performance.OsuPerformanceCalculator;
import lt.ekgame.beatmap_analyzer.performance.Performance;
import lt.ekgame.beatmap_analyzer.performance.scores.Score;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class OsuDifficulty extends Difficulty {

    private double aimDiff, speedDiff;

    private List<Double> aimStrains;
    private List<Double> speedStrains;

    public OsuDifficulty(OsuBeatmap beatmap, Mods mods, double starDiff, double aimDiff, double speedDiff, List<Double> aimStrains, List<Double> speedStrains) {
        super(beatmap, mods, starDiff, null);
        this.aimDiff = aimDiff;
        this.speedDiff = speedDiff;
        this.aimStrains = aimStrains;
        this.speedStrains = speedStrains;
    }

    private static List<Double> mergeStrains(List<Double> aimStrains, List<Double> speedStrains) {
        List<Double> overall = new ArrayList<Double>();
        Iterator<Double> aimIterator = aimStrains.iterator();
        Iterator<Double> speedIterator = speedStrains.iterator();
        while (aimIterator.hasNext() && speedIterator.hasNext()) {
            Double aimStrain = aimIterator.next();
            Double speedStrain = speedIterator.next();
            overall.add(aimStrain + speedStrain + Math.abs(speedStrain - aimStrain) * OsuDifficultyCalculator.EXTREME_SCALING_FACTOR);
        }
        return overall;
    }

    public List<Double> getAimStrains() {
        return aimStrains;
    }

    public List<Double> getSpeedStrains() {
        return speedStrains;
    }

    public double getAim() {
        return aimDiff;
    }

    public double getSpeed() {
        return speedDiff;
    }

    public double getAR() {
        return ((OsuBeatmap) beatmap).getAR(mods);
    }

    public double getOD() {
        return ((OsuBeatmap) beatmap).getOD(mods);
    }

    public int getNumCircles() {
        int c = 0;
        for (HitObject o : ((OsuBeatmap) beatmap).getHitObjects()) {
            if (o instanceof OsuCircle) c++;
        }


        return c;


        //return (int) ((OsuBeatmap)beatmap).getHitObjects().stream().filter(o->o instanceof OsuCircle).count();
    }

    @Override
    public Performance getPerformance(Score score) {
        return new OsuPerformanceCalculator().calculate(this, score);
    }
}
