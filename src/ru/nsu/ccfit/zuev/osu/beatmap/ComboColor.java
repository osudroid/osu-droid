package ru.nsu.ccfit.zuev.osu.beatmap;

import ru.nsu.ccfit.zuev.osu.RGBColor;

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

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private ComboColor(ComboColor source) {
        super(new RGBColor(source.r(), source.g(), source.b()));

        index = source.index;
    }

    /**
     * Deep clones this combo color.
     *
     * @return The deep cloned combo color.
     */
    public ComboColor deepClone() {
        return new ComboColor(this);
    }

}
