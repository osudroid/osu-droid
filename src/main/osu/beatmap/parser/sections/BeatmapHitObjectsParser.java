package main.osu.beatmap.parser.sections;

import com.rian.difficultycalculator.beatmap.constants.HitObjectType;
import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPath;
import com.rian.difficultycalculator.beatmap.hitobject.SliderPathType;
import com.rian.difficultycalculator.beatmap.hitobject.Spinner;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rian.difficultycalculator.math.Vector2;

import java.util.ArrayList;
import java.util.List;

import main.osu.Utils;
import main.osu.beatmap.BeatmapData;
import main.osu.game.GameHelper;

/**
 * A parser for parsing a beatmap's hit objects section.
 */
public class BeatmapHitObjectsParser extends BeatmapSectionParser {
    private final boolean parseHitObjects;
    private final int stackDistance = 3;

    /**
     * @param parseHitObjects Whether to also parse information of hit objects (such as circles,
     *                        slider paths, and spinners).
     *                        <br>
     *                        Parsed hit objects will be added to the
     *                        <code>BeatmapHitObjectsManager</code> of a <code>BeatmapData</code>.
     *
     */
    public BeatmapHitObjectsParser(boolean parseHitObjects) {
        super();

        this.parseHitObjects = parseHitObjects;
    }

    @Override
    public boolean parse(BeatmapData data, String line) {
        final String[] pars = line.split(",");

        if (pars.length < 4) {
            // Malformed hit object
            return false;
        }

        data.rawHitObjects.add(line);

        if (!parseHitObjects) {
            return true;
        }

        int time = Utils.tryParseInt(pars[2], -1);
        if (time < 0) {
            return false;
        }
        time = data.getOffsetTime(time);

        HitObjectType type = HitObjectType.valueOf(Utils.tryParseInt(pars[3], -1) % 16);
        Vector2 position = new Vector2(
            Utils.tryParseFloat(pars[0], Float.NaN),
            Utils.tryParseFloat(pars[1], Float.NaN)
        );

        if (Double.isNaN(position.x) || Double.isNaN(position.y)) {
            return false;
        }

        HitObject object = null;

        if (type == HitObjectType.Normal || type == HitObjectType.NormalNewCombo) {
            object = createCircle(time, position);
        } else if (type == HitObjectType.Slider || type == HitObjectType.SliderNewCombo) {
            object = createSlider(data, time, position, pars);
        } else if (type == HitObjectType.Spinner) {
            object = createSpinner(data, time, pars);
        }

        if (object == null) {
            return false;
        }

        data.hitObjects.add(object);

        return true;
    }

    /**
     * Applies note stacking to hit objects.
     *
     * Used for beatmaps version 6 or later.
     *
     * @param data The beatmap data with hit objects to apply note stacking to.
     */
    public void applyStacking(BeatmapData data, int startIndex, int endIndex) {
        if (data.getFormatVersion() < 6) {
            applyStackingOld(data);
            return;
        }

        double timePreempt = GameHelper.ar2ms(data.difficulty.ar);
        double stackThreshold = timePreempt * data.general.stackLeniency;

        List<HitObject> objects = data.hitObjects.getObjects();

        int extendedEndIndex = endIndex;

        for (int i = endIndex; i >= startIndex; --i) {
            int stackBaseIndex = i;

            for (int n = stackBaseIndex + 1; n < objects.size(); ++n) {
                HitObject stackBaseObject = objects.get(n);
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
                    continue;
                }

                if (stackBaseObject.getPosition().getDistance(objectN.getPosition()) < stackDistance ||
                        (stackBaseObject instanceof Slider &&
                        stackBaseObject.getEndPosition().getDistance(objectN.getPosition()) < stackDistance)) {
                    stackBaseIndex = n;

                    // Hit objects after the specified update range haven't been reset yet
                    objectN.setStackHeight(0);
                }
            }

            if (stackBaseIndex > extendedEndIndex) {
                extendedEndIndex = stackBaseIndex;
                if (extendedEndIndex == objects.size() - 1) {
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
     * Applies note stacking to hit objects.
     *
     * Used for beatmaps version 5 or older.
     *
     * @param data The beatmap data with hit objects to apply note stacking to.
     */
    public void applyStackingOld(BeatmapData data) {
        if (data.getFormatVersion() > 5) {
            applyStacking(data, 0, data.rawHitObjects.size());
            return;
        }

        List<HitObject> objects = data.hitObjects.getObjects();
        double timePreempt = GameHelper.ar2ms(data.difficulty.ar);
        double stackThreshold = timePreempt * data.general.stackLeniency;

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

    private HitCircle createCircle(int time, Vector2 position) {
        return new HitCircle(time, position);
    }

    private Slider createSlider(BeatmapData data, int time, Vector2 position, String[] pars) {
        // Handle malformed slider
        if (pars.length < 8) {
            return null;
        }

        String[] curvePointsData = pars[5].split("[|]");
        SliderPathType sliderType = SliderPathType.parse(curvePointsData[0].charAt(0));
        ArrayList<Vector2> curvePoints = new ArrayList<>();
        for (int i = 1; i < curvePointsData.length; i++) {
            String[] curvePointData = curvePointsData[i].split(":");
            Vector2 curvePointPosition = new Vector2(
                    Utils.tryParseFloat(curvePointData[0], Float.NaN),
                    Utils.tryParseFloat(curvePointData[1], Float.NaN)
            );

            if (Double.isNaN(curvePointPosition.x) || Double.isNaN(curvePointPosition.y)) {
                return null;
            }

            curvePoints.add(curvePointPosition);
        }

        int repeat = Utils.tryParseInt(pars[6], -1);
        float rawLength = Utils.tryParseFloat(pars[7], Float.NaN);
        if (repeat < 0 || Float.isNaN(rawLength)) {
            return null;
        }

        SliderPath path = new SliderPath(sliderType, curvePoints, rawLength);
        TimingControlPoint timingControlPoint = data.timingPoints.timing.controlPointAt(time);
        DifficultyControlPoint difficultyControlPoint = data.timingPoints.difficulty.controlPointAt(time);

        return new Slider(
                time,
                position,
                timingControlPoint,
                difficultyControlPoint,
                repeat,
                path,
                data.difficulty.sliderMultiplier,
                data.difficulty.sliderTickRate,
                // Prior to v8, speed multipliers don't adjust for how many ticks are generated over the same distance.
                // this results in more (or less) ticks being generated in <v8 maps for the same time duration.
                data.getFormatVersion() < 8 ? 1 / difficultyControlPoint.speedMultiplier : 1
        );
    }

    private Spinner createSpinner(BeatmapData data, int time, String[] pars) {
        int endTime = Utils.tryParseInt(pars[5], -1);
        if (endTime < 0) {
            return null;
        }
        endTime = data.getOffsetTime(endTime);

        return new Spinner(time, endTime);
    }
}
