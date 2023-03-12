package main.osu.beatmap.parser.sections;

import main.osu.Utils;
import main.osu.beatmap.BeatmapData;
import main.osu.game.BreakPeriod;

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
                data.events.breaks.add(new BreakPeriod(
                        data.getOffsetTime(Utils.tryParseInt(pars[1], 0)),
                        data.getOffsetTime(Utils.tryParseInt(pars[2], 0))
                ));
            }
        }
    }
}
