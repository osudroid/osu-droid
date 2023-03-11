package main.osu.beatmap.parser.sections;

import main.osu.Utils;
import main.osu.beatmap.BeatmapData;
import main.osu.beatmap.constants.BeatmapCountdown;
import main.osu.beatmap.constants.SampleBank;

/**
 * A parser for parsing a beatmap's general section.
 */
public class BeatmapGeneralParser extends BeatmapKeyValueSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "AudioFilename":
                data.general.audioFilename = p[1];
                break;
            case "AudioLeadIn":
                data.general.audioLeadIn = Utils.tryParseInt(p[1], data.general.audioLeadIn);
                break;
            case "PreviewTime":
                data.general.previewTime = Utils.tryParseInt(p[1], data.general.previewTime);
                break;
            case "Countdown":
                data.general.countdown = BeatmapCountdown.parse(p[1]);
                break;
            case "SampleSet":
                data.general.sampleBank = SampleBank.parse(p[1]);
                break;
            case "StackLeniency":
                data.general.stackLeniency = Utils.tryParseFloat(p[1], data.general.stackLeniency);
                break;
        }
    }
}
