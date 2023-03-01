package com.rian.difficultycalculator.attributes;

/**
 * Represents a beatmap section at which the strains of objects are considerably high.
 */
public final class HighStrainSection {
    /**
     * The index of the first object in this section with respect to the full beatmap.
     */
    public final int firstObjectIndex;

    /**
     * The index of the last object in this section with respect to the full beatmap.
     */
    public final int lastObjectIndex;

    /**
     * The summed strain of this section.
     */
    public final double sumStrain;

    /**
     * @param firstObjectIndex The index of the first object in this section with respect to the full beatmap.
     * @param lastObjectIndex The index of the last object in this section with respect to the full beatmap.
     * @param sumStrain The summed strain of this section.
     */
    public HighStrainSection(int firstObjectIndex, int lastObjectIndex, double sumStrain) {
        this.firstObjectIndex = firstObjectIndex;
        this.lastObjectIndex = lastObjectIndex;
        this.sumStrain = sumStrain;
    }
}
