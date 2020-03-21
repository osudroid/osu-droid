package lt.ekgame.beatmap_analyzer.difficulty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoCircle;
import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoCircle.TaikoColor;
import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoObject;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class TaikoDifficultyCalculator implements DifficultyCalculator {

    public static final double STAR_SCALING_FACTOR = 0.04125;
    public static final double DECAY_WEIGHT = 0.9;
    public static final int STRAIN_STEP = 400;

    public static final double DECAY_BASE = 0.30;
    public static final double COLOR_CHANGE_BONUS = 0.75;

    public static final double RHYTHM_CHANGE_BONUS = 1.0;
    public static final double RHYTHM_CHANGE_BASE_THRESHOLD = 0.2;
    public static final double RHYTHM_CHANGE_BASE = 2.0;

    @Override
    public TaikoDifficulty calculate(Mods mods, Beatmap beatmap) {
        TaikoBeatmap bm = (TaikoBeatmap) beatmap;
        double timeRate = mods.getSpeedMultiplier();
        List<TaikoObject> hitObjects = bm.getHitObjects();
        List<DifficultyObject> objects = generateDifficultyObjects(hitObjects, timeRate);
        List<Double> strains = calculateStrains(objects, timeRate);
        List<Double> strainsOriginal = new ArrayList<>(strains);
        double difficulty = calculateDifficulty(strains);
        return new TaikoDifficulty(bm, mods, difficulty, strainsOriginal);
    }

    private List<DifficultyObject> generateDifficultyObjects(List<TaikoObject> hitObjects, double timeRate) {
        List<DifficultyObject> difficultyObjects = new ArrayList<DifficultyObject>();
        for (int i = 0; i < hitObjects.size(); i++) {
            difficultyObjects.add(i, new DifficultyObject(hitObjects.get(i)));
        }

        Collections.sort(difficultyObjects, new Comparator<DifficultyObject>() {
            @Override
            public int compare(TaikoDifficultyCalculator.DifficultyObject a, TaikoDifficultyCalculator.DifficultyObject b) {
                return a.object.getStartTime() - b.object.getStartTime();
            }
        });

        //hitObjects.stream()
        //.map(o->new DifficultyObject(o))
        //.sorted((a, b)-> a.object.getStartTime() - b.object.getStartTime())
        //.collect(Collectors.toList());

        DifficultyObject previous = null;
        for (DifficultyObject current : difficultyObjects) {
            if (previous != null)
                current.calculateStrain(previous, timeRate);
            previous = current;
        }

        return difficultyObjects;
    }

    private List<Double> calculateStrains(List<DifficultyObject> difficultyObjects, double timeRate) {
        List<Double> highestStrains = new ArrayList<>();
        double realStrainStep = STRAIN_STEP * timeRate;
        double intervalEnd = realStrainStep;
        double maxStrain = 0;

        DifficultyObject previous = null;
        for (DifficultyObject current : difficultyObjects) {
            while (current.object.getStartTime() > intervalEnd) {
                highestStrains.add(maxStrain);
                if (previous != null) {
                    double decay = Math.pow(DECAY_BASE, (double) (intervalEnd - previous.object.getStartTime()) / 1000);
                    maxStrain = previous.strain * decay;
                }
                intervalEnd += realStrainStep;
            }
            maxStrain = Math.max(maxStrain, current.strain);
            previous = current;
        }

        return highestStrains;
    }

    public double calculateDifficulty(List<Double> strains) {
        Collections.sort(strains, new Comparator<Double>() {
            @Override
            public int compare(Double a, Double b) {
                return (int) Math.signum(b - a);
            }
        });
        //(a,b)->(int)(Math.signum(b-a)));
        double difficulty = 0, weight = 1;
        for (double strain : strains) {
            difficulty += weight * strain;
            weight *= DECAY_WEIGHT;
        }
        return difficulty * STAR_SCALING_FACTOR;
    }

    enum ColorSwitch {
        NONE, EVEN, ODD
    }

    class DifficultyObject {

        private TaikoObject object;
        private double strain = 1;
        private double timeElapsed;
        private boolean isBlue = false;
        private int sameColorChain = 0;
        private ColorSwitch lastColorSwitch = ColorSwitch.NONE;

        DifficultyObject(TaikoObject object) {
            this.object = object;

            // XXX: can drumrolls be blue?
            if (object instanceof TaikoCircle)
                isBlue = ((TaikoCircle) object).getColor() == TaikoColor.BLUE;
        }

        private void calculateStrain(DifficultyObject previous, double timeRate) {
            timeElapsed = (object.getStartTime() - previous.object.getStartTime()) / timeRate;
            double decay = Math.pow(DECAY_BASE, timeElapsed / 1000);
            double addition = 1;


            boolean isClose = object.getStartTime() - previous.object.getStartTime() < 1000;
            if (object instanceof TaikoCircle && previous.object instanceof TaikoCircle && isClose) {
                addition += colorChangeAddition(previous);
                addition += rhythmChangeAddition(previous);
            }

            double additionFactor = 1;
            if (timeElapsed < 50)
                additionFactor = 0.4 + 0.6 * timeElapsed / 50;

            strain = previous.strain * decay + addition * additionFactor;
        }

        private double colorChangeAddition(DifficultyObject previous) {
            if (isBlue != previous.isBlue) {
                lastColorSwitch = previous.sameColorChain % 2 == 0 ? ColorSwitch.EVEN : ColorSwitch.ODD;

                if (previous.lastColorSwitch != ColorSwitch.NONE && previous.lastColorSwitch != lastColorSwitch)
                    return COLOR_CHANGE_BONUS;
            } else {
                lastColorSwitch = previous.lastColorSwitch;
                sameColorChain = previous.sameColorChain + 1;
            }
            return 0;
        }

        private double rhythmChangeAddition(DifficultyObject previous) {
            if (timeElapsed == 0 || previous.timeElapsed == 0)
                return 0;

            double timeElapsedRatio = Math.max(previous.timeElapsed / timeElapsed, timeElapsed / previous.timeElapsed);
            if (timeElapsedRatio > 8)
                return 0;

            double difference = (Math.log(timeElapsedRatio) / Math.log(RHYTHM_CHANGE_BASE)) % 1.0;
            if (difference > RHYTHM_CHANGE_BASE_THRESHOLD && difference < 1 - RHYTHM_CHANGE_BASE_THRESHOLD)
                return RHYTHM_CHANGE_BONUS;

            return 0;
        }
    }
}
