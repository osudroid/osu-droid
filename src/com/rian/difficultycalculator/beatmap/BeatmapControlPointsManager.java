package com.rian.difficultycalculator.beatmap;

import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPointManager;
import com.rian.difficultycalculator.beatmap.timings.EffectControlPointManager;
import com.rian.difficultycalculator.beatmap.timings.SampleControlPointManager;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPointManager;

/**
 * A manager for beatmap control points.
 */
public class BeatmapControlPointsManager {
    /**
     * The manager for timing control points of this beatmap.
     */
    public final TimingControlPointManager timing = new TimingControlPointManager();

    /**
     * The manager for difficulty control points of this beatmap.
     */
    public final DifficultyControlPointManager difficulty = new DifficultyControlPointManager();

    /**
     * The manager for effect control points of this beatmap.
     */
    public final EffectControlPointManager effect = new EffectControlPointManager();

    /**
     * The manager for sample control points of this beatmap.
     */
    public final SampleControlPointManager sample = new SampleControlPointManager();
}
