package lt.ekgame.beatmap_analyzer.difficulty;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuObject;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuSpinner;
import lt.ekgame.beatmap_analyzer.utils.Mods;
import lt.ekgame.beatmap_analyzer.utils.Vec2;

public class OsuDifficultyCalculator implements DifficultyCalculator {

    public static final double DECAY_BASE[] = {0.27, 0.10};
    public static final double STRIAN_DECAY_BASE[] = {0.3, 0.12};
    public static final double WEIGHT_SCALING[] = {1400, 26.25};
    public static final double STAR_SCALING_FACTOR = 0.005;// 0.0675;
    public static final double EXTREME_SCALING_FACTOR = 0.5;
    public static final float PLAYFIELD_WIDTH = 512;
    public static final double DECAY_WEIGHT = 0.9;
    public static final double SPEED_DECAY_WEIGHT = 0.91;

    public static final double ALMOST_DIAMETER = 60;
    public static final double STREAM_SPACING = 90;
    public static final double SINGLE_SPACING = 125;

    public static final double TIMES_STREAM = 200;

    public static final int STRAIN_STEP = 300;

    public static final float CIRCLE_SIZE_BUFF_TRESHOLD = 30;

    public static final byte DIFF_SPEED = 0;
    public static final byte DIFF_AIM = 1;

    public static final double[] STRAIN_D_STEP = {3, 3};
    public static final double[] STRAIN_D_RATE = {0.95, 0.95};
    public static final double STRAIN_TIME_WIDTH = 500;


    @Override
    public OsuDifficulty calculate(Mods mods, Beatmap beatmap) {
        OsuBeatmap bm = (OsuBeatmap) beatmap;

        double timeRate = mods.getSpeedMultiplier();
        List<OsuObject> hitObjects = bm.getHitObjects();
        List<DifficultyObject> objects = generateDifficultyObjects(hitObjects, bm.getCS(mods), timeRate);

        List<Double> aimStrains = calculateStrains(objects, DIFF_AIM, timeRate);
        List<Double> speedStrains = calculateStrains(objects, DIFF_SPEED, timeRate);

        //List<Double> aimStrainsOriginal = new ArrayList<>(aimStrains);
        //List<Double> speedStrainsOriginal = new ArrayList<>(speedStrains);

        double aimDifficulty =
                //calculateNewDifficulty(objects, DIFF_AIM, timeRate);
                calculateDifficulty(aimStrains);
        double speedDifficulty =
                //calculateNewDifficulty(objects, DIFF_SPEED, timeRate);
                calculateDifficulty(speedStrains, SPEED_DECAY_WEIGHT);
        Log.i("new-diff", 0 + ": " + speedDifficulty);
        double starDifficulty = aimDifficulty + speedDifficulty + Math.abs(speedDifficulty - aimDifficulty) * EXTREME_SCALING_FACTOR;
        return new OsuDifficulty(bm, mods, starDifficulty, aimDifficulty, speedDifficulty, null, null);
        //return new OsuDifficulty(bm, mods, starDifficulty, aimDifficulty, speedDifficulty, aimStrainsOriginal, speedStrainsOriginal);
    }

    public double calculateNewDifficulty(List<DifficultyObject> objects, int type, double timeRate) {

        ArrayList<ArrayList<DifficultyObject>> classifyObj = new ArrayList<>();

        double currentTime = 0;
        double strainTimeWidth = STRAIN_TIME_WIDTH * timeRate;
        double blockTime = strainTimeWidth;

        Iterator<DifficultyObject> iterator = objects.iterator();
        ArrayList<DifficultyObject> tmpList = new ArrayList<>();
        classifyObj.add(tmpList);
        while (iterator.hasNext()) {
            final DifficultyObject object = iterator.next();
            currentTime = object.object.getStartTime();
            while (currentTime > blockTime) {
                //新的时间段开始
                blockTime += STRAIN_TIME_WIDTH;
                tmpList = new ArrayList<>();
                classifyObj.add(tmpList);
            }
            tmpList.add(object);
        }

        double[] avgDiff = new double[classifyObj.size()];
        for (int i = 0; i < avgDiff.length; i++) {
            final ArrayList<DifficultyObject> tmp = classifyObj.get(i);
            if (tmp.size() == 0) {
                avgDiff[i] = 0;
            } else {
                for (DifficultyObject object : tmp) {
                    avgDiff[i] += object.strains[type];
                }
                avgDiff[i] /= tmp.size();
            }
        }

        Arrays.sort(avgDiff);

        double totalDiff = 0;
        double weight = 1;
        double preDiff = avgDiff[avgDiff.length - 1];
        for (int i = avgDiff.length - 1; i >= 0; i--) {
            final double diff = avgDiff[i];
            totalDiff += diff * weight;

            //难度对weight的减少值
            weight *= Math.pow(STRAIN_D_RATE[type], Math.min(1, (preDiff - diff) / STRAIN_D_STEP[type]));
            //物件对weight的减少值
            weight *= 0.9;

            if (weight > 0.01) {
                //Log.i("new-diff", type + ": w :" + weight);
                //Log.i("new-diff", type + ": d :" + diff);
            }
            preDiff = diff;
        }
        totalDiff *= STAR_SCALING_FACTOR;
        //Log.i("new-diff", type + ": " + totalDiff);
        return totalDiff;
    }

    public List<DifficultyObject> generateDifficultyObjects(OsuBeatmap bm) {
        return generateDifficultyObjects(bm.getHitObjects(), bm.getCS(), 1);
    }

    public List<DifficultyObject> generateDifficultyObjects(List<OsuObject> hitObjects, double csRating, double timeRate) {
        double radius = (PLAYFIELD_WIDTH / 16) * (1 - 0.7 * (csRating - 5) / 5);

        List<DifficultyObject> difficultyObjects = new ArrayList<DifficultyObject>(hitObjects.size());

        for (int i = 0; i < hitObjects.size(); i++) {
            difficultyObjects.add(i, new DifficultyObject(hitObjects.get(i), radius, i));
        }
        Collections.sort(difficultyObjects, new Comparator<DifficultyObject>() {
            @Override
            public int compare(OsuDifficultyCalculator.DifficultyObject o1, OsuDifficultyCalculator.DifficultyObject o2) {
                return o1.object.getStartTime() - o2.object.getStartTime();
            }
        });

        //hitObjects.stream()
        //	.map(o->new DifficultyObject(o, radius))
        //	.sorted((a, b)-> a.object.getStartTime() - b.object.getStartTime())
        //	.collect(Collectors.toList());

        DifficultyObject previous = null;
        for (DifficultyObject current : difficultyObjects) {
            if (previous != null)
                current.calculateStrains(previous, timeRate);
            previous = current;
        }

        return difficultyObjects;
    }

    private List<Double> calculateStrains(List<DifficultyObject> objects, byte difficultyType, double timeRate) {
        List<Double> highestStrains = new ArrayList<>();
        double realStrainStep = STRAIN_STEP * timeRate;
        double intervalEnd = realStrainStep;
        double maxStrain = 0;

        DifficultyObject previous = null;
        for (DifficultyObject current : objects) {
            while (current.object.getStartTime() > intervalEnd) {
                highestStrains.add(maxStrain);
                if (previous != null) {
                    double decay = Math.pow(
                            STRIAN_DECAY_BASE[difficultyType],
                            (double) (intervalEnd - previous.object.getStartTime()) / 1000
                    );
                    maxStrain = previous.strains[difficultyType] * decay;
                }
                intervalEnd += realStrainStep;
            }
            maxStrain = Math.max(maxStrain, current.strains[difficultyType]);
            previous = current;
        }
        return highestStrains;
    }

    @Override
    public double calculateDifficulty(List<Double> strains) {
        return calculateDifficulty(strains, DECAY_WEIGHT);
    }

    public double calculateDifficulty(List<Double> strains, double decayWeight) {
        Collections.sort(strains, new Comparator<Double>() {
            @Override
            public int compare(Double a, Double b) {
                return (int) (Math.signum(b - a));
            }
        });
        //(a,b)->(int)(Math.signum(b-a)));

        double difficulty = 0, weight = 1;
        for (double strain : strains) {
            difficulty += weight * strain;
            weight *= decayWeight;
        }


        return Math.sqrt(difficulty) * STAR_SCALING_FACTOR;
    }

    public class DifficultyObject {

        public double[] strains = {1, 1};
        public int idx;
        private OsuObject object;
        private Vec2 normStart;//, normEnd;

        DifficultyObject(OsuObject object, double radius, int idx) {
            this.object = object;
            this.idx = idx;

            double scalingFactor = 52 / radius;
            if (radius < CIRCLE_SIZE_BUFF_TRESHOLD)
                scalingFactor *= 1 + Math.min(CIRCLE_SIZE_BUFF_TRESHOLD - radius, 5) / 50;

            normStart = object.getPosition().scale(scalingFactor);
            //normEnd = normStart;
        }

        private void calculateStrains(DifficultyObject previous, double timeRate) {
            calculateStrain(previous, timeRate, DIFF_SPEED);
            calculateStrain(previous, timeRate, DIFF_AIM);
        }

        private void calculateStrain(DifficultyObject previous, double timeRate, byte difficultyType) {
            double res = 0;
            double timeElapsed = (object.getStartTime() - previous.object.getStartTime()) / timeRate;
            double decay = Math.pow(DECAY_BASE[difficultyType], timeElapsed / 1000f);
            double scaling = WEIGHT_SCALING[difficultyType];

            if (!(object instanceof OsuSpinner)) {
                double distance = normStart.distance(previous.normStart);
                res = spacingWeight(distance, timeElapsed, difficultyType) * scaling;
            }

            res /= Math.max(timeElapsed, 50);
            strains[difficultyType] = previous.strains[difficultyType] * decay + res;
        }

        private double spacingWeight(double distance, double time, byte difficultyType) {
            if (difficultyType == DIFF_SPEED) {
                double v = 0;
                if (distance > SINGLE_SPACING) {
                    v = 2.3;
                } else if (distance > STREAM_SPACING) {
                    v = 1.7 + 0.6 * (distance - STREAM_SPACING) / (SINGLE_SPACING - STREAM_SPACING);
                } else if (distance > ALMOST_DIAMETER) {
                    v = 1.2 + 0.5 * (distance - ALMOST_DIAMETER) / (STREAM_SPACING - ALMOST_DIAMETER);
                } else if (distance > ALMOST_DIAMETER / 2) {
                    v = 0.9 + 0.3 * (distance - ALMOST_DIAMETER / 2) / (ALMOST_DIAMETER / 2);
                } else v = 0.9;
                if (time < TIMES_STREAM) {
                    double t = time / TIMES_STREAM;
                    v *= 1.9 * (1 - t) + t * 0.5;
                } else {
                    v *= 0.5f;
                }
                return v;
            } else if (difficultyType == DIFF_AIM) {
                if (time < TIMES_STREAM * 2) {
                    double t = time / TIMES_STREAM / 2;
                    distance *= 1.1 * (1 - t) + 0.9 * t;
                } else {
                    distance *= 0.9;
                }
                return distance;
            } else
                return 0;
        }
    }
}
