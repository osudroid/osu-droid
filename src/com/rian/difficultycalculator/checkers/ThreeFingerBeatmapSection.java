package com.rian.difficultycalculator.checkers;

import com.rian.difficultycalculator.attributes.HighStrainSection;

/**
 * An extended strain section for assigning dragged sections in three-finger detection.
 */
public final class ThreeFingerBeatmapSection extends HighStrainSection {
    /**
     * Whether this beatmap section is dragged.
     */
    public boolean isDragged;

    /**
     * The index of the cursor that is dragging this section. <code>-1</code> if this section is not dragged.
     */
    public int dragFingerIndex = -1;

    /**
     * @param section The existing high strain section.
     */
    public ThreeFingerBeatmapSection(HighStrainSection section) {
        super(section.firstObjectIndex, section.lastObjectIndex, section.sumStrain);
    }
}
