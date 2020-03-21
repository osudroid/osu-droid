package test.tpdifficulty.tp;

import android.graphics.PointF;

import test.tpdifficulty.hitobject.HitObject;
import test.tpdifficulty.hitobject.HitObjectType;
import test.tpdifficulty.hitobject.Slider;

/**
 * Created by Fuuko on 2015/5/30.
 */
public class tpHitObject implements Comparable<tpHitObject> {
    // Factor by how much speed / aim strain decays per second. Those values are results of tweaking a lot and taking into account general feedback.
    public static double[] DECAY_BASE = {0.3, 0.15}; // Opinionated observation: Speed is easier to maintain than accurate jumps.

    private static double ALMOST_DIAMETER = 90; // Almost the normed diameter of a circle (104 osu pixel). That is -after- position transforming.

    // Pseudo threshold values to distinguish between "singles" and "streams". Of course the border can not be defined clearly, therefore the algorithm
    // has a smooth transition between those values. They also are based on tweaking and general feedback.
    private static double STREAM_SPACING_TRESHOLD = 110;
    private static double SINGLE_SPACING_TRESHOLD = 125;

    // Scaling values for weightings to keep aim and speed difficulty in balance. Found from testing a very large map pool (containing all ranked maps) and keeping the
    // average values the same.
    private static double[] SPACING_WEIGHT_SCALING = {1400, 26.25};

    // In milliseconds. The smaller the value, the more accurate sliders are approximated. 0 leads to an infinite loop, so use something bigger.
    private static int LAZY_SLIDER_STEP_LENGTH = 1;
    public HitObject BaseHitObject;
    public double[] Strains = {1, 1};
    private PointF NormalizedStartPosition;
    private PointF NormalizedEndPosition;
    private float LazySliderLengthFirst = 0;
    private float LazySliderLengthSubsequent = 0;
    public tpHitObject(HitObject BaseHitObject, float CircleRadius) {
        this.BaseHitObject = BaseHitObject;

        // We will scale everything by this factor, so we can assume a uniform CircleSize among beatmaps.
        float ScalingFactor = (52.0f / CircleRadius);
        NormalizedStartPosition = new PointF(BaseHitObject.getPos().x * ScalingFactor, BaseHitObject.getPos().y * ScalingFactor);
        if (BaseHitObject.getType() == HitObjectType.Slider) {
            PointF endPos = ((Slider) BaseHitObject).getPoss().get(((Slider) BaseHitObject).getPoss().size() - 1);
            NormalizedEndPosition = new PointF(endPos.x * ScalingFactor, endPos.y * ScalingFactor);
        } else {
            NormalizedEndPosition = new PointF(BaseHitObject.getPos().x * ScalingFactor, BaseHitObject.getPos().y * ScalingFactor);
        }
    }

    // Caution: The subjective values are strong with this one
    private static double SpacingWeight(double distance, DifficultyType Type) {

        switch (Type) {
            case Speed:

            {
                double Weight;

                if (distance > SINGLE_SPACING_TRESHOLD) {
                    Weight = 2.5;
                } else if (distance > STREAM_SPACING_TRESHOLD) {
                    Weight = 1.6 + 0.9 * (distance - STREAM_SPACING_TRESHOLD) / (SINGLE_SPACING_TRESHOLD - STREAM_SPACING_TRESHOLD);
                } else if (distance > ALMOST_DIAMETER) {
                    Weight = 1.2 + 0.4 * (distance - ALMOST_DIAMETER) / (STREAM_SPACING_TRESHOLD - ALMOST_DIAMETER);
                } else if (distance > ALMOST_DIAMETER / 2) {
                    Weight = 0.95 + 0.25 * (distance - (ALMOST_DIAMETER / 2)) / (ALMOST_DIAMETER / 2);
                } else {
                    Weight = 0.95;
                }

                return Weight;
            }


            case Aim:

                return Math.pow(distance, 0.99);


            // Should never happen.
            default:
                return 0;
        }
    }

    public void CalculateStrains(tpHitObject PreviousHitObject) {
        CalculateSpecificStrain(PreviousHitObject, DifficultyType.Speed);
        CalculateSpecificStrain(PreviousHitObject, DifficultyType.Aim);
    }

    private void CalculateSpecificStrain(tpHitObject PreviousHitObject, DifficultyType Type) {
        double Addition = 0;
        double TimeElapsed = BaseHitObject.getStartTime() - PreviousHitObject.BaseHitObject.getStartTime();
        double Decay = Math.pow(DECAY_BASE[Type.value()], TimeElapsed / 1000);

        if (BaseHitObject.getType() == HitObjectType.Spinner) {
            // Do nothing for spinners
        } else if (BaseHitObject.getType() == HitObjectType.Slider) {
            switch (Type) {
                case Speed:

                    // For speed strain we treat the whole slider as a single spacing entity, since "Speed" is about how hard it is to click buttons fast.
                    // The spacing weight exists to differentiate between being able to easily alternate or having to single.
                    Addition =
                            SpacingWeight(PreviousHitObject.LazySliderLengthFirst +
                                    PreviousHitObject.LazySliderLengthSubsequent * PreviousHitObject.BaseHitObject.getRepeat() +
                                    DistanceTo(PreviousHitObject), Type) *
                                    SPACING_WEIGHT_SCALING[Type.value()];
                    break;


                case Aim:

                    // For Aim strain we treat each slider segment and the jump after the end of the slider as separate jumps, since movement-wise there is no difference
                    // to multiple jumps.
                    Addition =
                            (
                                    SpacingWeight(PreviousHitObject.LazySliderLengthFirst, Type) +
                                            SpacingWeight(PreviousHitObject.LazySliderLengthSubsequent, Type) * PreviousHitObject.BaseHitObject.getRepeat() +
                                            SpacingWeight(DistanceTo(PreviousHitObject), Type)
                            ) *
                                    SPACING_WEIGHT_SCALING[Type.value()];
                    break;
            }

        } else if (BaseHitObject.getType() == HitObjectType.Normal) {
            Addition = SpacingWeight(DistanceTo(PreviousHitObject), Type) * SPACING_WEIGHT_SCALING[Type.value()];
        }

        // Scale addition by the time, that elapsed. Filter out HitObjects that are too close to be played anyway to avoid crazy values by division through close to zero.
        // You will never find maps that require this amongst ranked maps.
        Addition /= Math.max(TimeElapsed, 50);

        Strains[Type.value()] = PreviousHitObject.Strains[Type.value()] * Decay + Addition;
    }


    public double DistanceTo(tpHitObject other) {
        // Scale the distance by circle size.
        return Math.sqrt(Math.pow((NormalizedStartPosition.x - other.NormalizedEndPosition.x), 2) +
                Math.pow((NormalizedStartPosition.y - other.NormalizedEndPosition.y), 2));
    }

    public int compareTo(tpHitObject o1) {
        return this.BaseHitObject.getStartTime() - o1.BaseHitObject.getStartTime();
    }
}
