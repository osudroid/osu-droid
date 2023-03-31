package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import java.util.Collections;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.ComboColor;

/**
 * A parser for parsing a beatmap's colors section.
 */
public class BeatmapColorParser extends BeatmapKeyValueSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);
        String[] s = p[1].split(",");

        if (s.length != 3 && s.length != 4) {
            throw new UnsupportedOperationException("Color specified in incorrect format (should be R,G,B or R,G,B,A)");
        }

        RGBColor color = new RGBColor(
                parseInt(s[0]),
                parseInt(s[1]),
                parseInt(s[2])
        );

        if (p[0].startsWith("Combo")) {
            int index = Utils.tryParseInt(p[0].substring(5), data.colors.comboColors.size() + 1);
            data.colors.comboColors.add(new ComboColor(index, color));
            Collections.sort(data.colors.comboColors, (a, b) -> Integer.compare(a.index, b.index));
        }

        if (p[0].startsWith("SliderBorder")) {
            data.colors.sliderBorderColor = color;
        }
    }
}
