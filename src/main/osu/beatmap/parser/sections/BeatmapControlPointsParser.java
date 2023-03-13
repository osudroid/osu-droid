package main.osu.beatmap.parser.sections;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.constants.SampleBank;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.EffectControlPoint;
import com.rian.difficultycalculator.beatmap.timings.SampleControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

import main.osu.Utils;
import main.osu.beatmap.BeatmapData;

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

        int time = Utils.tryParseInt(pars[0], -1);
        if (time < 0) {
            return false;
        }
        time = data.getOffsetTime(time);

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

        SampleBank sampleBank = data.general.sampleBank;
        if (pars.length >= 4) {
            sampleBank = SampleBank.parse(pars[3]);
        }

        int customSampleBank = 0;
        if (pars.length >= 5) {
            customSampleBank = Utils.tryParseInt(pars[4], customSampleBank);
        }

        int sampleVolume = data.general.sampleVolume;
        if (pars.length >= 6) {
            sampleVolume = Utils.tryParseInt(pars[5], sampleVolume);
        }

        boolean timingChange = true;
        if (pars.length >= 7) {
            timingChange = pars[6].equals("1");
        }

        boolean kiaiMode = false;
        if (pars.length >= 8) {
            int effectFlags = Utils.tryParseInt(pars[7], 0);
            kiaiMode = (effectFlags & 1) > 0;
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

        manager.effect.add(new EffectControlPoint(time, kiaiMode));
        manager.sample.add(new SampleControlPoint(time, sampleBank, sampleVolume, customSampleBank));

        return true;
    }
}
