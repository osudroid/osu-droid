package com.rian.difficultycalculator.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds data that can be used to calculate osu!droid performance points as well
 * as doing some analysis using the replay of a score.
 */
public class ExtendedRimuDifficultyAttributes extends RimuDifficultyAttributes {
    /**
     * Possible sections at which the player can use three fingers on.
     */
    public List<HighStrainSection> possibleThreeFingeredSections = Collections.unmodifiableList(new ArrayList<>());

    /**
     * Sliders that are considered difficult.
     */
    public List<DifficultSlider> difficultSliders = Collections.unmodifiableList(new ArrayList<>());

    /**
     * Describes how much of flashlight difficulty is contributed to by hit circles or sliders.
     * <br><br>
     * A value closer to 1 indicates most of flashlight difficulty is contributed by hit circles.
     * <br><br>
     * A value closer to 0 indicates most of flashlight difficulty is contributed by sliders.
     */
    public double flashlightSliderFactor;

    /**
     * Describes how much of visual difficulty is contributed to by hit circles or sliders.
     * <br><br>
     * A value closer to 1 indicates most of visual difficulty is contributed by hit circles.
     * <br><br>
     * A value closer to 0 indicates most of visual difficulty is contributed by sliders.
     */
    public double visualSliderFactor;
}
