package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.game.BreakPeriod;

/**
 * A parser for parsing a beatmap's events section.
 */
public class BeatmapEventsParser extends BeatmapSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        final String[] pars = line.split("\\s*,\\s*");

        if (pars.length >= 3) {
            if (line.startsWith("0,0")) {
                data.events.backgroundFilename = pars[2].substring(1, pars[2].length() - 1);
            }

            if (line.startsWith("2") || line.startsWith("Break")) {
                int start = data.getOffsetTime(parseInt(pars[1]));
                int end = Math.max(start, data.getOffsetTime(parseInt(pars[2])));
                data.events.breaks.add(new BreakPeriod(start, end));
            }
        }

        if (pars.length >= 5 && line.startsWith("3")) {
            data.events.backgroundColor = new RGBColor(
                    parseInt(pars[2]),
                    parseInt(pars[3]),
                    parseInt(pars[4])
            );
        }
    }
}
