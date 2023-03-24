package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's difficulty section.
 */
public class BeatmapDifficultyParser extends BeatmapKeyValueSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "CircleSize":
                data.difficulty.cs = Utils.tryParseFloat(p[1], data.difficulty.cs);
                break;
            case "OverallDifficulty":
                data.difficulty.od = Utils.tryParseFloat(p[1], data.difficulty.od);
                if (Float.isNaN(data.difficulty.ar)) {
                    data.difficulty.ar = data.difficulty.od;
                }
                break;
            case "ApproachRate":
                data.difficulty.ar = Utils.tryParseFloat(p[1], Float.isNaN(data.difficulty.ar) ? data.difficulty.od : data.difficulty.ar);
                break;
            case "HPDrainRate":
                data.difficulty.hp = Utils.tryParseFloat(p[1], data.difficulty.hp);
                break;
            case "SliderMultiplier":
                data.difficulty.sliderMultiplier = Utils.tryParseDouble(p[1], data.difficulty.sliderMultiplier);
                break;
            case "SliderTickRate":
                data.difficulty.sliderTickRate = Utils.tryParseDouble(p[1], data.difficulty.sliderTickRate);
        }

        return true;
    }
}
