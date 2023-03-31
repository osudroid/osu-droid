package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's difficulty section.
 */
public class BeatmapDifficultyParser extends BeatmapKeyValueSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "CircleSize":
                data.difficulty.cs = parseFloat(p[1]);
                break;
            case "OverallDifficulty":
                data.difficulty.od = parseFloat(p[1]);
                if (Float.isNaN(data.difficulty.ar)) {
                    data.difficulty.ar = data.difficulty.od;
                }
                break;
            case "ApproachRate":
                data.difficulty.ar = parseFloat(p[1]);
                break;
            case "HPDrainRate":
                data.difficulty.hp = parseFloat(p[1]);
                break;
            case "SliderMultiplier":
                data.difficulty.sliderMultiplier = parseDouble(p[1]);
                break;
            case "SliderTickRate":
                data.difficulty.sliderTickRate = parseDouble(p[1]);
                break;
        }
    }
}
