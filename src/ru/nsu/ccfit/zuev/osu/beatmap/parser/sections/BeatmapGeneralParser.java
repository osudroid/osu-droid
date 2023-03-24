package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank;

/**
 * A parser for parsing a beatmap's general section.
 */
public class BeatmapGeneralParser extends BeatmapKeyValueSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "AudioFilename":
                data.general.audioFilename = p[1];
                break;
            case "AudioLeadIn":
                data.general.audioLeadIn = Utils.tryParseInt(p[1], data.general.audioLeadIn);
                break;
            case "PreviewTime":
                data.general.previewTime = data.getOffsetTime(Utils.tryParseInt(p[1], data.general.previewTime));
                break;
            case "Countdown":
                data.general.countdown = BeatmapCountdown.parse(p[1]);
                break;
            case "SampleSet":
                data.general.sampleBank = SampleBank.parse(p[1]);
                break;
            case "SampleVolume":
                data.general.sampleVolume = Utils.tryParseInt(p[1], data.general.sampleVolume);
                break;
            case "StackLeniency":
                data.general.stackLeniency = Utils.tryParseFloat(p[1], data.general.stackLeniency);
                break;
            case "LetterboxInBreaks":
                data.general.letterboxInBreaks = p[1].equals("1");
            case "Mode":
                return p[1].equals("0");
        }

        return true;
    }
}
