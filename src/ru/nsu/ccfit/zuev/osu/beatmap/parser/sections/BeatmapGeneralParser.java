package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapCountdown;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.SampleBank;

/**
 * A parser for parsing a beatmap's general section.
 */
public class BeatmapGeneralParser extends BeatmapKeyValueSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "AudioFilename" -> data.general.audioFilename = p[1];
            case "AudioLeadIn" -> data.general.audioLeadIn = parseInt(p[1]);
            case "PreviewTime" -> data.general.previewTime = data.getOffsetTime(parseInt(p[1]));
            case "Countdown" -> data.general.countdown = BeatmapCountdown.parse(p[1]);
            case "SampleSet" -> data.general.sampleBank = SampleBank.parse(p[1]);
            case "SampleVolume" -> data.general.sampleVolume = parseInt(p[1]);
            case "StackLeniency" -> data.general.stackLeniency = parseFloat(p[1]);
            case "LetterboxInBreaks" -> data.general.letterboxInBreaks = p[1].equals("1");
            case "Mode" -> data.general.mode = parseInt(p[1]);
        }
    }
}
