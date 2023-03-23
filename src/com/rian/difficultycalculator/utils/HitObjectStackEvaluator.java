package com.rian.difficultycalculator.utils;

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;

import java.util.List;

/**
 * An evaluator for evaluating stack heights of objects.
 */
public final class HitObjectStackEvaluator {
    private static final int stackDistance = 3;

    public static void applyStandardStacking(int formatVersion, List<HitObject> objects, double ar,
                                             float stackLeniency) {
        applyStandardStacking(formatVersion, objects, ar, stackLeniency, 0, objects.size() - 1);
    }

    /**
     * Applies osu!standard note stacking to hit objects.
     *
     * @param formatVersion The format version of the beatmap containing the hit objects.
     * @param objects       The hit objects to apply stacking to.
     * @param ar            The calculated approach rate of the beatmap.
     * @param stackLeniency The multiplier for the threshold in time where hit objects
     *                      placed close together stack, ranging from 0 to 1.
     * @param startIndex    The minimum index bound of the hit object to apply stacking to.
     * @param endIndex      The maximum index bound of the hit object to apply stacking to.
     */
    public static void applyStandardStacking(int formatVersion, List<HitObject> objects, double ar,
                                             float stackLeniency, int startIndex, int endIndex) {
        if (objects.isEmpty()) {
            return;
        }

        if (formatVersion < 6) {
            // Use the old version of stacking algorithm for beatmap version 5 or lower.
            applyStandardStackingOld(objects, ar, stackLeniency);
            return;
        }

        double timePreempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);
        double stackThreshold = timePreempt * stackLeniency;

        int extendedEndIndex = endIndex;

        if (endIndex < objects.size() - 1) {
            // Extend the end index to include objects they are stacked on
            for (int i = endIndex; i >= startIndex; --i) {
                int stackBaseIndex = i;

                for (int n = stackBaseIndex + 1; n < objects.size(); n++) {
                    HitObject stackBaseObject = objects.get(stackBaseIndex);
                    if (stackBaseObject instanceof Spinner) {
                        break;
                    }

                    HitObject objectN = objects.get(n);
                    if (objectN instanceof Spinner) {
                        continue;
                    }

                    double endTime = stackBaseObject.getStartTime();
                    if (stackBaseObject instanceof HitObjectWithDuration) {
                        endTime = ((HitObjectWithDuration) stackBaseObject).getEndTime();
                    }

                    if (objectN.getStartTime() - endTime > stackThreshold) {
                        // We are no longer within stacking range of the next object.
                        break;
                    }

                    if (stackBaseObject.getPosition().getDistance(objectN.getPosition()) < stackDistance ||
                            (stackBaseObject instanceof Slider && stackBaseObject.getEndPosition().getDistance(objectN.getPosition())  < stackDistance)) {
                        stackBaseIndex = n;

                        // HitObjects after the specified update range haven't been reset yet
                        objectN.setStackHeight(0);
                    }
                }

                if (stackBaseIndex > extendedEndIndex) {
                    extendedEndIndex = stackBaseIndex;
                    if (extendedEndIndex == objects.size() - 1)
                        break;
                }
            }
        }

        // Reverse pass for stack calculation.
        int extendedStartIndex = startIndex;

        for (int i = extendedEndIndex; i > startIndex; --i) {
            int n = i;

            // We should check every note which has not yet got a stack.
            // Consider the case we have two inter-wound stacks and this will make sense.
            //
            // o <-1      o <-2
            //  o <-3      o <-4
            //
            // We first process starting from 4 and handle 2,
            // then we come backwards on the i loop iteration until we reach 3 and handle 1.
            // 2 and 1 will be ignored in the i loop because they already have a stack value.
            HitObject objectI = objects.get(i);
            if (objectI.getStackHeight() != 0 || objectI instanceof Spinner) {
                continue;
            }

            // If this object is a hit circle, then we enter this "special" case.
            // It either ends with a stack of hit circles only, or a stack of hit circles that are underneath a slider.
            // Any other case is handled by the "instanceof Slider" code below this.
            if (objectI instanceof HitCircle) {
                while (--n >= 0) {
                    HitObject objectN = objects.get(n);
                    if (objectN instanceof Spinner) {
                        continue;
                    }

                    double endTime = objectN.getStartTime();
                    if (objectN instanceof HitObjectWithDuration) {
                        endTime = ((HitObjectWithDuration) objectN).getEndTime();
                    }

                    if (objectI.getStartTime() - endTime > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break;
                    }

                    // Hit objects before the specified update range haven't been reset yet
                    if (n < extendedStartIndex) {
                        objectN.setStackHeight(0);
                        extendedStartIndex = n;
                    }

                    // This is a special case where hit circles are moved DOWN and RIGHT (negative stacking) if they are under the *last* slider in a stacked pattern.
                    // o==o <- slider is at original location
                    //     o <- hitCircle has stack of -1
                    //      o <- hitCircle has stack of -2
                    if (objectN instanceof Slider && objectN.getEndPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        int offset = objectI.getStackHeight() - objectN.getStackHeight() + 1;

                        for (int j = n + 1; j <= i; ++j) {
                            // For each object which was declared under this slider, we will offset it to appear *below* the slider end (rather than above).
                            HitObject objectJ = objects.get(j);
                            if (objectN.getEndPosition().getDistance(objectJ.getPosition()) < stackDistance) {
                                objectJ.setStackHeight(objectJ.getStackHeight() - offset);
                            }
                        }

                        // We have hit a slider. We should restart calculation using this as the new base.
                        // Breaking here will mean that the slider still has a stack count of 0, so will be handled in the i-outer-loop.
                        break;
                    }

                    if (objectN.getPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        // Keep processing as if there are no sliders. If we come across a slider, this gets cancelled out.
                        // NOTE: Sliders with start positions stacking are a special case that is also handled here.
                        objectN.setStackHeight(objectI.getStackHeight() + 1);
                        objectI = objectN;
                    }
                }
            } else if (objectI instanceof Slider) {
                // We have hit the first slider in a possible stack.
                // From this point on, we ALWAYS stack positive regardless.
                while (--n >= startIndex) {
                    HitObject objectN = objects.get(n);
                    if (objectN instanceof Spinner) {
                        continue;
                    }

                    if (objectI.getStartTime() - objectN.getStartTime() > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break;
                    }

                    if (objectN.getEndPosition().getDistance(objectI.getPosition()) < stackDistance) {
                        objectN.setStackHeight(objectI.getStackHeight() + 1);
                        objectI = objectN;
                    }
                }
            }
        }
    }

    /**
     * Applies osu!standard note stacking to hit objects.
     * <br><br>
     * Used for beatmaps version 5 or older.
     *
     * @param objects The hit objects to apply stacking to.
     * @param ar The calculated approach rate of the beatmap.
     * @param stackLeniency The multiplier for the threshold in time where hit objects
     *                      placed close together stack, ranging from 0 to 1.
     */
    private static void applyStandardStackingOld(List<HitObject> objects, double ar, float stackLeniency) {
        double timePreempt = (ar <= 5) ? (1800 - 120 * ar) : (1950 - 150 * ar);
        double stackThreshold = timePreempt * stackLeniency;

        for (int i = 0; i < objects.size(); ++i) {
            HitObject currentObject = objects.get(i);

            if (currentObject.getStackHeight() != 0 && !(currentObject instanceof Slider)) {
                continue;
            }

            int sliderStack = 0;
            double startTime = currentObject.getStartTime();
            if (currentObject instanceof HitObjectWithDuration) {
                startTime = ((HitObjectWithDuration) currentObject).getEndTime();
            }

            for (int j = i + 1; j < objects.size(); ++j) {
                if (objects.get(j).getStartTime() - stackThreshold > startTime) {
                    break;
                }

                if (objects.get(j).getPosition().getDistance(currentObject.getPosition()) < stackDistance) {
                    currentObject.setStackHeight(currentObject.getStackHeight() + 1);
                    startTime = objects.get(j).getStartTime();

                    if (objects.get(j) instanceof HitObjectWithDuration) {
                        startTime = ((HitObjectWithDuration) objects.get(j)).getEndTime();
                    }
                } else if (objects.get(j).getPosition().getDistance(currentObject.getEndPosition()) < stackDistance) {
                    // Case for sliders - bump notes down and right, rather than up and left.
                    ++sliderStack;
                    objects.get(j).setStackHeight(objects.get(j).getStackHeight() - sliderStack);
                    startTime = objects.get(j).getStartTime();

                    if (objects.get(j) instanceof HitObjectWithDuration) {
                        startTime = ((HitObjectWithDuration) objects.get(j)).getEndTime();
                    }
                }
            }
        }
    }
}
