package ru.nsu.ccfit.zuev.osu.beatmap.sections;

/**
 * Contains difficulty settings of a beatmap.
 */
public class BeatmapDifficulty {
    /**
     * The approach rate of this beatmap.
     */
    public float ar = Float.NaN;

    /**
     * The circle size of this beatmap.
     */
    public float cs = 5;

    /**
     * The overall difficulty of this beatmap.
     */
    public float od = 5;

    /**
     * The health drain rate of this beatmap.
     */
    public float hp = 5;

    /**
     * The base slider velocity in hundreds of osu! pixels per beat.
     */
    public double sliderMultiplier = 1;

    /**
     * The amount of slider ticks per beat.
     */
    public double sliderTickRate = 1;

    public BeatmapDifficulty() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapDifficulty(BeatmapDifficulty source) {
        ar = source.ar;
        od = source.od;
        cs = source.cs;
        hp = source.hp;
        sliderMultiplier = source.sliderMultiplier;
        sliderTickRate = source.sliderTickRate;
    }

    /**
     * Deep clones this instance.
     *
     * @return The deep cloned instance.
     */
    public BeatmapDifficulty deepClone() {
        return new BeatmapDifficulty(this);
    }
}
