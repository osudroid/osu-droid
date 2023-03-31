package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's timing points section.
 */
public class BeatmapControlPointsParser extends BeatmapSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        final String[] pars = line.split(",");

        if (pars.length < 2) {
            throw new UnsupportedOperationException("Malformed timing point");
        }

        int time = data.getOffsetTime(parseInt(pars[0]));

        // msPerBeat is allowed to be NaN to handle an edge case in which some
        // beatmaps use NaN slider velocity to disable slider tick generation.
        double msPerBeat = parseDouble(pars[1], true);

        int timeSignature = 4;
        if (pars.length >= 3) {
            timeSignature = parseInt(pars[2]);
        }

        if (timeSignature < 1) {
            throw new UnsupportedOperationException("The numerator of a time signature must be positive");
        }

        data.rawTimingPoints.add(line);

        boolean timingChange = true;
        if (pars.length >= 7) {
            timingChange = pars[6].equals("1");
        }

        BeatmapControlPointsManager manager = data.timingPoints;

        if (timingChange) {
            if (Double.isNaN(msPerBeat)) {
                throw new UnsupportedOperationException("Beat length cannot be NaN in a timing control point");
            }

            manager.timing.add(new TimingControlPoint(time, msPerBeat, timeSignature));
        }

        manager.difficulty.add(new DifficultyControlPoint(
                time,
                // If msPerBeat is NaN, speedMultiplier should still be 1 because all comparisons against NaN are false.
                msPerBeat < 0 ? 100 / -msPerBeat : 1,
                !Double.isNaN(msPerBeat)
        ));
    }
}
