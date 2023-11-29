package ru.nsu.ccfit.zuev.osu.beatmap.sections;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.game.BreakPeriod;

/**
 * Contains beatmap events.
 */
public class BeatmapEvents {

    /**
     * The file name of this beatmap's background.
     */
    public String backgroundFilename;

    /**
     * The file name of this beatmap's background video.
     */
    public String videoFilename;

    /**
     * The beatmap's background video start time in milliseconds.
     */
    public int videoStartTime;

    /**
     * The breaks this beatmap has.
     */
    public ArrayList<BreakPeriod> breaks = new ArrayList<>();

    /**
     * The background color of this beatmap.
     */
    public RGBColor backgroundColor;

    public BeatmapEvents() {
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapEvents(BeatmapEvents source) {
        backgroundFilename = source.backgroundFilename;

        for (BreakPeriod breakPeriod : source.breaks) {
            breaks.add(new BreakPeriod(breakPeriod.getStart(), breakPeriod.getStart() + breakPeriod.getLength()));
        }

        backgroundColor = source.backgroundColor != null ? new RGBColor(backgroundColor) : null;
        videoFilename = source.videoFilename;
        videoStartTime = source.videoStartTime;
    }

    /**
     * Deep clones this instance.
     *
     * @return The deep cloned instance.
     */
    public BeatmapEvents deepClone() {
        return new BeatmapEvents(this);
    }

}
