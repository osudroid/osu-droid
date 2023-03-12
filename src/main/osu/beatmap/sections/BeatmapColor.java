package main.osu.beatmap.sections;

import java.util.ArrayList;

import main.osu.RGBColor;
import main.osu.beatmap.ComboColor;

/**
 * Contains information about combo and skin colors of a beatmap.
 */
public class BeatmapColor {
    /**
     * The combo colors of this beatmap.
     */
    public ArrayList<ComboColor> comboColors = new ArrayList<>();

    /**
     * The color of the slider border.
     */
    public RGBColor sliderBorderColor;
}
