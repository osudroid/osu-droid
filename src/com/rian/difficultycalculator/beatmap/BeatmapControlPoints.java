package com.rian.difficultycalculator.beatmap;

import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPointManager;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPointManager;

/**
 * A class containing information about timing (control) points of a beatmap.
 */
public class BeatmapControlPoints {
    /**
     * The manager for timing control points of this beatmap.
     */
    public final TimingControlPointManager timing = new TimingControlPointManager();

    /**
     * The manager for difficulty control points of this beatmap.
     */
    public final DifficultyControlPointManager difficulty = new DifficultyControlPointManager();
}
