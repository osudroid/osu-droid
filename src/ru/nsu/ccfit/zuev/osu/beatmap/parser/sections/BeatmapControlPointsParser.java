package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's timing points section.
 */
public class BeatmapControlPointsParser extends BeatmapSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        final String[] pars = line.split(",");

        if (pars.length < 2) {
            // Malformed timing point
            return false;
        }

        int time = data.getOffsetTime(Utils.tryParseInt(pars[0], 0));

        // msPerBeat is allowed to be NaN to handle an edge case in which some
        // beatmaps use NaN slider velocity to disable slider tick generation.
        double msPerBeat = Utils.tryParseDouble(pars[1], Double.NaN);

        int timeSignature = 4;
        if (pars.length >= 3) {
            timeSignature = Utils.tryParseInt(pars[2], timeSignature);
        }

        if (timeSignature < 1) {
            // The numerator of a time signature must be positive
            return false;
        }

        data.rawTimingPoints.add(line);

        boolean timingChange = true;
        if (pars.length >= 7) {
            timingChange = pars[6].equals("1");
        }

        BeatmapControlPointsManager manager = data.timingPoints;

        if (timingChange) {
            if (Double.isNaN(msPerBeat)) {
                // Beat length cannot be NaN in a timing control point
                return false;
            }

            manager.timing.add(new TimingControlPoint(time, msPerBeat, timeSignature));
        }

        manager.difficulty.add(new DifficultyControlPoint(
                time,
                // If msPerBeat is NaN, speedMultiplier should still be 1 because all comparisons against NaN are false.
                msPerBeat < 0 ? 100 / -msPerBeat : 1,
                !Double.isNaN(msPerBeat)
        ));

        return true;
    }
}
