package main.osu.beatmap.sections;

import java.util.ArrayList;

import main.osu.game.BreakPeriod;

/**
 * Contains beatmap events.
 */
public class BeatmapEvents {
    /**
     * The file name of this beatmap's background.
     */
    public String backgroundFilename;

    /**
     * The breaks this beatmap has.
     */
    public ArrayList<BreakPeriod> breaks = new ArrayList<>();
}
