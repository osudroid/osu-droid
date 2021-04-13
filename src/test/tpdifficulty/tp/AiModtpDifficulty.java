package test.tpdifficulty.tp;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import test.tpdifficulty.hitobject.HitObject;

/**
 * Created by Fuuko on 2015/5/30.
 */
public class AiModtpDifficulty {
    private static double STAR_SCALING_FACTOR = 0.045 / 0.666;
    private static double EXTREME_SCALING_FACTOR = 0.5;
    private static float PLAYFIELD_WIDTH = 512;
    // In milliseconds. For difficulty calculation we will only look at the highest strain value in each time interval of size STRAIN_STEP.
    // This is to eliminate higher influence of stream over aim by simply having more HitObjects with high strain.
    // The higher this value, the less strains there will be, indirectly giving long beatmaps an advantage.
    private static double STRAIN_STEP = 400;
    // The weighting of each strain value decays to 0.9 * it's previous value
    private static double DECAY_WEIGHT = 0.9;
    ArrayList<tpHitObject> tpHitObjects;
    private double SpeedStars, AimStars, StarRating;
    private double SpeedDifficulty, AimDifficulty;

    private double CalculateDifficulty(DifficultyType Type) {
        // Find the highest strain value within each strain step
        ArrayList<Double> HighestStrains = new ArrayList<Double>();
        double IntervalEndTime = STRAIN_STEP;
        double MaximumStrain = 0; // We need to keep track of the maximum strain in the current interval

        tpHitObject PreviousHitObject = null;
        for (tpHitObject hitObject : tpHitObjects) {
            // While we are beyond the current interval push the currently available maximum to our strain list
            while (hitObject.BaseHitObject.getStartTime() > IntervalEndTime) {
                HighestStrains.add(MaximumStrain);

                // The maximum strain of the next interval is not zero by default! We need to take the last hitObject we encountered, take its strain and apply the decay
                // until the beginning of the next interval.
                if (PreviousHitObject == null) {
                    MaximumStrain = 0;
                } else {
                    double Decay = Math.pow(tpHitObject.DECAY_BASE[Type.value()], (double) (IntervalEndTime - PreviousHitObject.BaseHitObject.getStartTime()) / 1000);
                    MaximumStrain = PreviousHitObject.Strains[Type.value()] * Decay;
                }

                // Go to the next time interval
                IntervalEndTime += STRAIN_STEP;
            }

            // Obtain maximum strain
            if (hitObject.Strains[Type.value()] > MaximumStrain) {
                MaximumStrain = hitObject.Strains[Type.value()];
            }

            PreviousHitObject = hitObject;
        }
        /* don't forget to add the last strain interval */
        HighestStrains.add(MaximumStrain);
        
        // Build the weighted sum over the highest strains for each interval
        double Difficulty = 0;
        double Weight = 1;
        Collections.sort(HighestStrains, Collections.reverseOrder()); // Sort from highest to lowest strain.

        for (double Strain : HighestStrains) {
            Difficulty += Weight * Strain;
            Weight *= DECAY_WEIGHT;
        }

        return Difficulty;
    }

    private Boolean CalculateStrainValues() {
        // Traverse hitObjects in pairs to calculate the strain value of NextHitObject from the strain value of CurrentHitObject and environment.
        Iterator<tpHitObject> HitObjectsIterator = tpHitObjects.iterator();
        if (HitObjectsIterator.hasNext() == false) {
            return false;
        }

        tpHitObject CurrentHitObject = HitObjectsIterator.next();
        tpHitObject NextHitObject;

        // First hitObject starts at strain 1. 1 is the default for strain values, so we don't need to set it here. See tpHitObject.

        while (HitObjectsIterator.hasNext()) {
            NextHitObject = HitObjectsIterator.next();
            NextHitObject.CalculateStrains(CurrentHitObject);
            CurrentHitObject = NextHitObject;
        }

        return true;
    }
    public void CalculateAll(ArrayList<HitObject> hitObjects, float circleSize) {
        CalculateAll(hitObjects,circleSize,1.0f);
    }
    public void CalculateAll(ArrayList<HitObject> hitObjects, float circleSize, float speed) {
        // Fill our custom tpHitObject class, that carries additional information
        tpHitObjects = new ArrayList<tpHitObject>(hitObjects.size());
        // The Max CS in osu!droid is 17.62, but in pc OSU! is about 12.14. so I map 10-17.62(gameplay CS) to 10-12.14(star calculate CS)
        float cs = Math.min(circleSize, 17.62f);
        if (cs > 10.0f) {
            cs = 10.0f + (cs - 10.0f) * (12.14f - 10.0f) / (17.62f - 10.0f);
        }
        float CircleRadius = (PLAYFIELD_WIDTH / 16.0f) * (1.0f - 0.7f * (cs - 5.0f) / 5.0f);
        for (HitObject hitObject : hitObjects) {
            tpHitObject hitObj = new tpHitObject(hitObject, CircleRadius);
            hitObj.BaseHitObject.setStartTime((int)(hitObj.BaseHitObject.getStartTime() / speed));
            hitObj.BaseHitObject.setEndTime((int)(hitObj.BaseHitObject.getEndTime() / speed));
            tpHitObjects.add(hitObj);
        }

        // Sort tpHitObjects by StartTime of the HitObjects - just to make sure. Not using CompareTo, since it results in a crash (HitObjectBase inherits MarshalByRefObject)
        Collections.sort(tpHitObjects);

        // Calculates the flow angle of the Hitobjects
        tpHitObject prev1 = null;
        tpHitObject prev2 = null;
        int i = 0;
        for(tpHitObject hitObject : tpHitObjects){
            if(i >= 2){
                PointF v1 = new PointF(prev2.getNormPosStart().x - prev1.getNormPosEnd().x,
                    prev2.getNormPosStart().y - prev1.getNormPosEnd().y);
                PointF v2 = new PointF(hitObject.getNormPosStart().x - prev1.getNormPosEnd().x,
                    hitObject.getNormPosStart().y - prev1.getNormPosEnd().y);
                double dot = v1.x * v2.x + v1.y * v2.y;
                double det = v1.x * v2.y - v1.y * v2.x;
                hitObject.angle = Math.abs(Math.atan2(det, dot));
            } 
            else {
                hitObject.angle = Double.NaN;
            }
            prev2 = prev1;
            prev1 = hitObject;
            ++i;
        }

        if (CalculateStrainValues() == false) {
            System.out.println("Could not compute strain values. Aborting difficulty calculation.");
            return;
        }


        SpeedDifficulty = CalculateDifficulty(DifficultyType.Speed);
        AimDifficulty = CalculateDifficulty(DifficultyType.Aim);

        // OverallDifficulty is not considered in this algorithm and neither is HpDrainRate. That means, that in this form the algorithm determines how hard it physically is
        // to play the map, assuming, that too much of an error will not lead to a death.
        // It might be desirable to include OverallDifficulty into map difficulty, but in my personal opinion it belongs more to the weighting of the actual peformance
        // and is superfluous in the beatmap difficulty rating.
        // If it were to be considered, then I would look at the hit window of normal HitCircles only, since Sliders and Spinners are (almost) "free" 300s and take map length
        // into account as well.

        System.out.println("Speed difficulty: " + SpeedDifficulty + " | Aim difficulty: " + AimDifficulty);

        // The difficulty can be scaled by any desired metric.
        // In osu!tp it gets squared to account for the rapid increase in difficulty as the limit of a human is approached. (Of course it also gets scaled afterwards.)
        // It would not be suitable for a star rating, therefore:

        // The following is a proposal to forge a star rating from 0 to 5. It consists of taking the square root of the difficulty, since by simply scaling the easier
        // 5-star maps would end up with one star.
        SpeedStars = Math.sqrt(SpeedDifficulty) * STAR_SCALING_FACTOR;
        AimStars = Math.sqrt(AimDifficulty) * STAR_SCALING_FACTOR;

        System.out.println("Speed stars: " + SpeedStars + " | Aim stars: " + AimStars);

        // Again, from own observations and from the general opinion of the community a map with high speed and low aim (or vice versa) difficulty is harder,
        // than a map with mediocre difficulty in both. Therefore we can not just add both difficulties together, but will introduce a scaling that favors extremes.
        StarRating = SpeedStars + AimStars + Math.abs(SpeedStars - AimStars) * EXTREME_SCALING_FACTOR;
        // Another approach to this would be taking Speed and Aim separately to a chosen power, which again would be equivalent. This would be more convenient if
        // the hit window size is to be considered as well.

        // Note: The star rating is tuned extremely tight! Airman (/b/104229) and Freedom Dive (/b/126645), two of the hardest ranked maps, both score ~4.66 stars.
        // Expect the easier kind of maps that officially get 5 stars to obtain around 2 by this metric. The tutorial still scores about half a star.
        // Tune by yourself as you please. ;)
        System.out.println("Total star rating: " + StarRating);
    }

    public double getAimDifficulty() {
        return AimDifficulty;
    }

    public double getSpeedStars() {
        return SpeedStars;
    }

    public double getAimStars() {
        return AimStars;
    }

    public double getStarRating() {
        return StarRating;
    }

    public double getSpeedDifficulty() {
        return SpeedDifficulty;
    }
}
