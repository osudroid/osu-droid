package main.osu.beatmap;

import main.osu.RGBColor;

/**
 * An extension to <code>RGBColor</code> specifically for combo colors.
 */
public class ComboColor extends RGBColor {
    /**
     * The index of this combo color.
     */
    public final int index;

    public ComboColor(int index, RGBColor color) {
        super(color);

        this.index = index;
    }
}
