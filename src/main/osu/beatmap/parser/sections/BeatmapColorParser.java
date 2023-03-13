package main.osu.beatmap.parser.sections;

import java.util.Arrays;
import java.util.Comparator;

import main.osu.RGBColor;
import main.osu.Utils;
import main.osu.beatmap.BeatmapData;
import main.osu.beatmap.ComboColor;

/**
 * A parser for parsing a beatmap's colors section.
 */
public class BeatmapColorParser extends BeatmapKeyValueSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);
        int[] s = Arrays.stream(p[1].split(",")).mapToInt(a -> Utils.tryParseInt(a, 0)).toArray();

        if (s.length != 3 && s.length != 4) {
            return false;
        }

        RGBColor color = new RGBColor(s[0], s[1], s[2]);

        if (p[0].startsWith("Combo")) {
            int index = Utils.tryParseInt(p[0].substring(5), data.colors.comboColors.size() + 1);
            data.colors.comboColors.add(new ComboColor(index, color));
            data.colors.comboColors.sort(Comparator.comparingInt(a -> a.index));

            return true;
        }

        if (p[0].startsWith("SliderBorder")) {
            data.colors.sliderBorderColor = color;
        }

        return true;
    }
}
