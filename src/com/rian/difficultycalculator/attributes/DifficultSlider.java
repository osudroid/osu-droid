package com.rian.difficultycalculator.attributes;

/**
 * Represents a slider that is considered difficult.
 */
public final class DifficultSlider {
    /**
     * The index of the slider in the beatmap.
     */
    public final int index;

    /**
     * The difficulty rating of this slider compared to other sliders, based on the velocity of the slider.
     * <br><br>
     * A value closer to 1 indicates that this slider is more difficult compared to most sliders.
     * <br><br>
     * A value closer to 0 indicates that this slider is easier compared to most sliders.
     */
    public double difficultyRating;

    /**
     * @param index The index of the slider in the beatmap.
     * @param difficultyRating The difficulty rating of this slider compared to other sliders, based on the velocity of the slider.
     */
    public DifficultSlider(int index, double difficultyRating) {
        this.index = index;
        this.difficultyRating = difficultyRating;
    }
}
