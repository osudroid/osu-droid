package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.game.BreakPeriod;

/**
 * A parser for parsing a beatmap's events section.
 */
public class BeatmapEventsParser extends BeatmapSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        final String[] pars = line.split("\\s*,\\s*");

        if (pars.length >= 3) {
            if (line.startsWith("0,0")) {
                data.events.backgroundFilename = pars[2].substring(1, pars[2].length() - 1);
            }

            if (line.startsWith("2") || line.startsWith("Break")) {
                int start = data.getOffsetTime(Utils.tryParseInt(pars[1], 0));
                int end = Math.max(start, data.getOffsetTime(Utils.tryParseInt(pars[2], 0)));
                data.events.breaks.add(new BreakPeriod(start, end));
            }
        }

        if (pars.length >= 5 && line.startsWith("3")) {
            data.events.backgroundColor = new RGBColor(
                    Utils.tryParseInt(pars[2], 0),
                    Utils.tryParseInt(pars[3], 0),
                    Utils.tryParseInt(pars[4], 0)
            );
        }

        return true;
    }
}
