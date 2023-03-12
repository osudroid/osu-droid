package com.rian.difficultycalculator.beatmap.timings;

import com.rian.difficultycalculator.beatmap.constants.SampleBank;

/**
 * Represents a control point that handles sample sounds.
 */
public class SampleControlPoint extends ControlPoint {
    /**
     * The sample bank at this control point.
     */
    public final SampleBank sampleBank;

    /**
     * The sample volume at this control point.
     */
    public final int sampleVolume;

    /**
     * The index of the sample bank, if this sample bank uses custom samples.
     *
     * If this is 0, the beatmap's sample should be used instead.
     */
    public final int customSampleBank;

    public SampleControlPoint(int time, SampleBank sampleBank, int sampleVolume, int customSampleBank) {
        super(time);

        this.sampleBank = sampleBank;
        this.sampleVolume = sampleVolume;
        this.customSampleBank = customSampleBank;
    }

    @Override
    public boolean isRedundant(ControlPoint existing) {
        return existing instanceof SampleControlPoint &&
                sampleBank == ((SampleControlPoint) existing).sampleBank &&
                sampleVolume == ((SampleControlPoint) existing).sampleVolume &&
                customSampleBank == ((SampleControlPoint) existing).customSampleBank;
    }
}
