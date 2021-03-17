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
    public double[] Strains = {0, 0};
    private PointF NormalizedStartPosition;
    private PointF NormalizedEndPosition;
    private float LazySliderLengthFirst = 0;
    private float LazySliderLengthSubsequent = 0;
    public double delta_time = 0.0;
    public double d_distance = 0.0;
    public double angle = 0.0;

    private final static double MIN_SPEED_BONUS = 75.0; // ~200BPM
    private final static double MAX_SPEED_BONUS = 45.0; // ~330BPM
    private final static double ANGLE_BONUS_SCALE = 90.0;
    private final static double AIM_TIMING_THRESHOLD = 107;
    private final static double SPEED_ANGLE_BONUS_BEGIN = 5 * Math.PI / 6; //if angle < this, buff speed
    private final static double AIM_ANGLE_BONUS_BEGIN = Math.PI / 3; //if angle > this, buff aim
    private final static double CIRCLESIZE_BUFF_THRESHOLD = 30.0;
	private final static double SPEED_BALANCING_FACTOR = 40;

    public tpHitObject(HitObject BaseHitObject, float CircleRadius) {
        this.BaseHitObject = BaseHitObject;

        // We will scale everything by this factor, so we can assume a uniform CircleSize among beatmaps.
        float ScalingFactor = (52.0f / CircleRadius);
        if (CircleRadius < CIRCLESIZE_BUFF_THRESHOLD)
        {
            ScalingFactor *= 1.0 +
                Math.min(CIRCLESIZE_BUFF_THRESHOLD - CircleRadius, 5.0) / 50.0;
        }
        NormalizedStartPosition = new PointF(BaseHitObject.getPos().x * ScalingFactor, BaseHitObject.getPos().y * ScalingFactor);
        if (BaseHitObject.getType() == HitObjectType.Slider) {
            PointF endPos = ((Slider) BaseHitObject).getCurvePoints().get(((Slider) BaseHitObject).getCurvePoints().size() - 1);
            NormalizedEndPosition = new PointF(endPos.x * ScalingFactor, endPos.y * ScalingFactor);
            
            float approxFollowCircleRadius = CircleRadius * 3;
            float sliderLength = (float)Math.sqrt(Math.pow((NormalizedStartPosition.x - NormalizedEndPosition.x), 2) +
            Math.pow((NormalizedStartPosition.y - NormalizedEndPosition.y), 2));
            if(sliderLength > approxFollowCircleRadius){
                LazySliderLengthFirst = sliderLength - approxFollowCircleRadius;
            }
            if(sliderLength > approxFollowCircleRadius * 2){
                LazySliderLengthSubsequent = sliderLength - approxFollowCircleRadius * 2;
            }
        } else {
            NormalizedEndPosition = new PointF(BaseHitObject.getPos().x * ScalingFactor, BaseHitObject.getPos().y * ScalingFactor);
        }
    }

    public PointF getNormPosStart(){
        return NormalizedStartPosition;
    }

    public PointF getNormPosEnd(){
        return NormalizedEndPosition;
    }

    // Caution: The subjective values are strong with this one
    // private static double SpacingWeight(double distance, DifficultyType Type) {

    //     switch (Type) {
    //         case Speed:

    //         {
    //             double Weight;

    //             if (distance > SINGLE_SPACING_TRESHOLD) {
    //                 Weight = 2.5;
    //             } else if (distance > STREAM_SPACING_TRESHOLD) {
    //                 Weight = 1.6 + 0.9 * (distance - STREAM_SPACING_TRESHOLD) / (SINGLE_SPACING_TRESHOLD - STREAM_SPACING_TRESHOLD);
    //             } else if (distance > ALMOST_DIAMETER) {
    //                 Weight = 1.2 + 0.4 * (distance - ALMOST_DIAMETER) / (STREAM_SPACING_TRESHOLD - ALMOST_DIAMETER);
    //             } else if (distance > ALMOST_DIAMETER / 2) {
    //                 Weight = 0.95 + 0.25 * (distance - (ALMOST_DIAMETER / 2)) / (ALMOST_DIAMETER / 2);
    //             } else {
    //                 Weight = 0.95;
    //             }

    //             return Weight;
    //         }


    //         case Aim:

    //             return Math.pow(distance, 0.99);


    //         // Should never happen.
    //         default:
    //             return 0;
    //     }
    // }

    public void CalculateStrains(tpHitObject PreviousHitObject) {
        CalculateSpecificStrain(PreviousHitObject, DifficultyType.Speed);
        CalculateSpecificStrain(PreviousHitObject, DifficultyType.Aim);
    }

    private void CalculateSpecificStrain(tpHitObject PreviousHitObject, DifficultyType Type) {
        double Addition = 0;
        double TimeElapsed = BaseHitObject.getStartTime() - PreviousHitObject.BaseHitObject.getStartTime();
        double Decay = Math.pow(DECAY_BASE[Type.value()], TimeElapsed / 1000);
        double Distance = DistanceTo(PreviousHitObject);
        delta_time = TimeElapsed;
        if (BaseHitObject.getType() == HitObjectType.Spinner) {
            // Do nothing for spinners
        } else if (BaseHitObject.getType() == HitObjectType.Slider) {
            d_distance = Distance;
            switch (Type) {
                case Speed:
                    // For speed strain we treat the whole slider as a single spacing entity, since "Speed" is about how hard it is to click buttons fast.
                    // The spacing weight exists to differentiate between being able to easily alternate or having to single.
                    // Addition =
                    //         SpacingWeight(PreviousHitObject.LazySliderLengthFirst +
                    //                 PreviousHitObject.LazySliderLengthSubsequent * PreviousHitObject.BaseHitObject.getRepeat() +
                    //                 Distance, Type) *
                    //                 SPACING_WEIGHT_SCALING[Type.value()];
                    Distance += PreviousHitObject.LazySliderLengthFirst +
                            PreviousHitObject.LazySliderLengthSubsequent * PreviousHitObject.BaseHitObject.getRepeat();
                    Addition = d_spacing_weight(Type, Distance, TimeElapsed, PreviousHitObject.d_distance, PreviousHitObject.delta_time, angle);
                    Addition *= SPACING_WEIGHT_SCALING[Type.value()];
                    break;

                    
                case Aim:
                    // For Aim strain we treat each slider segment and the jump after the end of the slider as separate jumps, since movement-wise there is no difference
                    // to multiple jumps.
                    // Addition =
                    //         (
                    //                 SpacingWeight(PreviousHitObject.LazySliderLengthFirst, Type) +
                    //                         SpacingWeight(PreviousHitObject.LazySliderLengthSubsequent, Type) * PreviousHitObject.BaseHitObject.getRepeat() +
                    //                         SpacingWeight(Distance, Type)
                    //         ) *
                    //                 SPACING_WEIGHT_SCALING[Type.value()];
                    Addition = d_spacing_weight(Type, Distance, TimeElapsed, PreviousHitObject.d_distance, PreviousHitObject.delta_time, angle);
                    Addition += d_spacing_weight(Type, PreviousHitObject.LazySliderLengthFirst, TimeElapsed, PreviousHitObject.d_distance, PreviousHitObject.delta_time, angle);
                    Addition += d_spacing_weight(Type, PreviousHitObject.LazySliderLengthSubsequent, TimeElapsed, PreviousHitObject.d_distance, PreviousHitObject.delta_time, angle) * PreviousHitObject.BaseHitObject.getRepeat();
                    Addition *= SPACING_WEIGHT_SCALING[Type.value()];
                    break;
            }

        } else if (BaseHitObject.getType() == HitObjectType.Normal) {
            d_distance = Distance;
            Addition = d_spacing_weight(Type, Distance, TimeElapsed, PreviousHitObject.d_distance, PreviousHitObject.delta_time, angle);
            Addition *= SPACING_WEIGHT_SCALING[Type.value()];
            //Addition = SpacingWeight(DistanceTo(PreviousHitObject), Type) * SPACING_WEIGHT_SCALING[Type.value()];
        }

        // Scale addition by the time, that elapsed. Filter out HitObjects that are too close to be played anyway to avoid crazy values by division through close to zero.
        // You will never find maps that require this amongst ranked maps.
        // but it works in d_spacing_weight(), so i use //
        // Addition /= Math.max(TimeElapsed, 50);

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

    //this method is Copy from https://github.com/Francesco149/koohii
    private double d_spacing_weight(DifficultyType type, double distance, double delta_time,
                                        double prev_distance, double prev_delta_time, double angle)
    {
        double strain_time = Math.max(delta_time, 50.0);
        double prev_strain_time = Math.max(prev_delta_time, 50.0);
        double angle_bonus;
        switch (type)
        {
            case Aim: {
                double result = 0.0;
                if (!Double.isNaN(angle) && angle > AIM_ANGLE_BONUS_BEGIN) {
                    angle_bonus = Math.sqrt(
                        Math.max(prev_distance - ANGLE_BONUS_SCALE, 0.0) *
                        Math.pow(Math.sin(angle - AIM_ANGLE_BONUS_BEGIN), 2.0) *
                        Math.max(distance - ANGLE_BONUS_SCALE, 0.0)
                    );
                    result = (
                        1.5 * Math.pow(Math.max(0.0, angle_bonus), 0.99) /
                        Math.max(AIM_TIMING_THRESHOLD, prev_strain_time)
                    );
                }
                double weighted_distance = Math.pow(distance, 0.99);
                return Math.max(result +
                    weighted_distance /
                    Math.max(AIM_TIMING_THRESHOLD, strain_time),
                    weighted_distance / strain_time);
            }

            case Speed: {
                distance = Math.min(distance, SINGLE_SPACING_TRESHOLD);
                delta_time = Math.max(delta_time, MAX_SPEED_BONUS);
                double speed_bonus = 1.0;
                if (delta_time < MIN_SPEED_BONUS) {
                    speed_bonus +=
                        Math.pow((MIN_SPEED_BONUS - delta_time) / SPEED_BALANCING_FACTOR, 2);
                }
                angle_bonus = 1.0;
                if (!Double.isNaN(angle) && angle < SPEED_ANGLE_BONUS_BEGIN) {
                    double s = Math.sin(1.5 * (SPEED_ANGLE_BONUS_BEGIN - angle));
                    angle_bonus += Math.pow(s, 2) / 3.57;
                    if (angle < Math.PI / 2.0) {
                        angle_bonus = 1.28;
                        if (distance < ANGLE_BONUS_SCALE && angle < Math.PI / 4.0) {
                            angle_bonus += (1.0 - angle_bonus) *
                                Math.min((ANGLE_BONUS_SCALE - distance) / 10.0, 1.0);
                        } else if (distance < ANGLE_BONUS_SCALE) {
                            angle_bonus += (1.0 - angle_bonus) *
                                Math.min((ANGLE_BONUS_SCALE - distance) / 10.0, 1.0) *
                                Math.sin((Math.PI / 2.0 - angle) * 4.0 / Math.PI);
                        }
                    }
                }
                return ((1 + (speed_bonus - 1) * 0.75) * angle_bonus *
                    (0.95 + speed_bonus * Math.pow(distance / SINGLE_SPACING_TRESHOLD, 3.5))) 
                    / strain_time;
            }
        }

        throw new UnsupportedOperationException(
            "this difficulty type does not exist"
        );
    }

}
