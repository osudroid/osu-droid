package com.rian.difficultycalculator.beatmap;

/**
 * A beatmap difficulty manager.
 */
public class BeatmapDifficultyManager {
    /**
     * The circle size of this beatmap.
     */
    private double cs = 5;

    /**
     * The approach of this beatmap.
     *
     * NaN initially.
     */
    private double ar = Double.NaN;

    /**
     * The overall difficulty of this beatmap.
     */
    private double od = 5;

    /**
     * The health drain rate of this beatmap.
     */
    private double hp = 5;

    /**
     * The base slider velocity in hundreds of osu! pixels per beat.
     */
    private double sliderMultiplier = 1;

    /**
     * The amount of slider ticks per beat.
     */
    private double sliderTickRate = 1;

    public BeatmapDifficultyManager() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapDifficultyManager(BeatmapDifficultyManager source) {
        cs = source.cs;
        ar = source.ar;
        od = source.od;
        hp = source.hp;
        sliderMultiplier = source.sliderMultiplier;
        sliderTickRate = source.sliderTickRate;
    }

    /**
     * Deep clones this difficulty manager.
     *
     * @return The deep cloned instance of this manager.
     */
    public BeatmapDifficultyManager deepClone() {
        return new BeatmapDifficultyManager(this);
    }

    /**
     * Gets the circle size of this beatmap.
     */
    public double getCS() {
        return cs;
    }

    /**
     * Sets the circle size of this beatmap.
     *
     * @param cs The new circle size.
     */
    public void setCS(double cs) {
        this.cs = cs;
    }

    /**
     * Gets the approach rate of this beatmap.
     */
    public double getAR() {
        return Double.isNaN(ar) ? od : ar;
    }

    /**
     * Sets the approach rate of this beatmap.
     *
     * @param ar The new approach rate.
     */
    public void setAR(double ar) {
        this.ar = ar;
    }

    /**
     * Gets the overall difficulty of this beatmap.
     */
    public double getOD() {
        return od;
    }

    /**
     * Sets the overall difficulty of this beatmap.
     *
     * @param od The new overall difficulty.
     */
    public void setOD(double od) {
        this.od = od;
    }

    /**
     * Gets the health drain rate of this beatmap.
     */
    public double getHP() {
        return hp;
    }

    /**
     * Sets the health drain rate of this beatmap.
     * 
     * @param hp The new health drain rate.
     */
    public void setHP(double hp) {
        this.hp = hp;
    }

    /**
     * Gets the base slider velocity in hundreds of osu! pixels per beat.
     */
    public double getSliderMultiplier() {
        return sliderMultiplier;
    }

    /**
     * Sets the base slider velocity in hundreds of osu! pixels per beat.
     * 
     * @param sliderMultiplier The new base slider velocity in hundreds of osu! pixels per beat.
     */
    public void setSliderMultiplier(double sliderMultiplier) {
        this.sliderMultiplier = sliderMultiplier;
    }

    /**
     * Gets the amount of slider ticks per beat.
     */
    public double getSliderTickRate() {
        return sliderTickRate;
    }

    /**
     * Sets the amount of slider ticks per beat.
     *
     * @param sliderTickRate The new amount of slider ticks per beat.
     */
    public void setSliderTickRate(double sliderTickRate) {
        this.sliderTickRate = sliderTickRate;
    }
}
