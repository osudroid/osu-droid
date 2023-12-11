package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import com.rian.difficultycalculator.beatmap.hitobject.*;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rian.difficultycalculator.math.Precision;
import com.rian.difficultycalculator.math.Vector2;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.HitObjectType;

import java.util.ArrayList;

/**
 * A parser for parsing a beatmap's hit objects section.
 */
public class BeatmapHitObjectsParser extends BeatmapSectionParser {

    @Override
    public void parse(BeatmapData data, String line) {
        final String[] pars = line.split(",");

        if (pars.length < 4) {
            throw new UnsupportedOperationException("Malformed hit object");
        }

        double time = data.getOffsetTime(parseDouble(pars[2]));

        HitObjectType type = HitObjectType.valueOf(parseInt(pars[3]) % 16);
        Vector2 position = new Vector2((int) parseFloat(pars[0], maxCoordinateValue), (int) parseFloat(pars[1], maxCoordinateValue));

        HitObject object = null;

        if (type == HitObjectType.Normal || type == HitObjectType.NormalNewCombo) {
            object = createCircle(time, position);
        } else if (type == HitObjectType.Slider || type == HitObjectType.SliderNewCombo) {
            object = createSlider(data, time, position, pars);
        } else if (type == HitObjectType.Spinner) {
            object = createSpinner(data, time, pars);
        }

        data.rawHitObjects.add(line);
        data.hitObjects.add(object);
    }

    private HitCircle createCircle(double time, Vector2 position) {
        return new HitCircle(time, position);
    }

    private Slider createSlider(BeatmapData data, double time, Vector2 position, String[] pars) throws UnsupportedOperationException {
        if (pars.length < 8) {
            throw new UnsupportedOperationException("Malformed slider");
        }

        int repeat = parseInt(pars[6]);
        double rawLength = Math.max(0, parseDouble(pars[7], maxCoordinateValue));

        if (repeat > 9000) {
            throw new UnsupportedOperationException("Repeat count is way too high");
        }

        String[] curvePointsData = pars[5].split("[|]");
        SliderPathType sliderType = SliderPathType.parse(curvePointsData[0].charAt(0));
        ArrayList<Vector2> curvePoints = new ArrayList<>();
        curvePoints.add(new Vector2(0));
        for (int i = 1; i < curvePointsData.length; i++) {
            String[] curvePointData = curvePointsData[i].split(":");
            Vector2 curvePointPosition = new Vector2((int) parseFloat(curvePointData[0], maxCoordinateValue), (int) parseFloat(curvePointData[1], maxCoordinateValue));

            curvePoints.add(curvePointPosition.subtract(position));
        }

        // A special case for old beatmaps where the first
        // control point is in the position of the slider.
        if (curvePoints.size() >= 2 && curvePoints.get(0).equals(curvePoints.get(1))) {
            curvePoints.remove(0);
        }

        // Edge-case rules (to match stable).
        if (sliderType == SliderPathType.PerfectCurve) {
            if (curvePoints.size() != 3) {
                sliderType = SliderPathType.Bezier;
            } else if (Precision.almostEqualsNumber(0, (curvePoints.get(1).y - curvePoints.get(0).y) * (curvePoints.get(2).x - curvePoints.get(0).x) - (curvePoints.get(1).x - curvePoints.get(0).x) * (curvePoints.get(2).y - curvePoints.get(0).y))) {
                // osu-stable special-cased co-linear perfect curves to a linear path
                sliderType = SliderPathType.Linear;
            }
        }

        SliderPath path = new SliderPath(sliderType, curvePoints, rawLength);
        TimingControlPoint timingControlPoint = data.timingPoints.timing.controlPointAt(time);
        DifficultyControlPoint difficultyControlPoint = data.timingPoints.difficulty.controlPointAt(time);

        return new Slider(time, position, timingControlPoint, difficultyControlPoint, repeat, path, data.difficulty.sliderMultiplier, data.difficulty.sliderTickRate,
                // Prior to v8, speed multipliers don't adjust for how many ticks are generated over the same distance.
                // this results in more (or less) ticks being generated in <v8 maps for the same time duration.
                data.getFormatVersion() < 8 ? 1 / difficultyControlPoint.speedMultiplier : 1, difficultyControlPoint.generateTicks);
    }

    private Spinner createSpinner(BeatmapData data, double time, String[] pars) {
        double endTime = data.getOffsetTime(parseInt(pars[5]));
        return new Spinner(time, endTime);
    }

}
