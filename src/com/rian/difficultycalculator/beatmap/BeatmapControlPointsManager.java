package com.rian.difficultycalculator.beatmap;

import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPointManager;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPointManager;

/**
 * A manager for beatmap control points.
 */
public class BeatmapControlPointsManager {
    /**
     * The manager for timing control points of this beatmap.
     */
    public final TimingControlPointManager timing;

    /**
     * The manager for difficulty control points of this beatmap.
     */
    public final DifficultyControlPointManager difficulty;

    public BeatmapControlPointsManager() {
        timing = new TimingControlPointManager();
        difficulty = new DifficultyControlPointManager();
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapControlPointsManager(BeatmapControlPointsManager source) {
        timing = source.timing.deepClone();
        difficulty = source.difficulty.deepClone();
    }

    /**
     * Deep clones this manager.
     *
     * @return The deep cloned manager.
     */
    public BeatmapControlPointsManager deepClone() {
        return new BeatmapControlPointsManager(this);
    }
}
