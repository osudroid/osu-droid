package main.osu.beatmap.sections;

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
    public float sliderMultiplier = 1;

    /**
     * The amount of slider ticks per beat.
     */
    public int sliderTickRate = 1;
}
