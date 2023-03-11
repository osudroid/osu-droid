package main.osu.beatmap;

import java.util.ArrayList;

import main.osu.beatmap.sections.BeatmapDifficulty;
import main.osu.beatmap.sections.BeatmapEvents;
import main.osu.beatmap.sections.BeatmapGeneral;
import main.osu.beatmap.sections.BeatmapMetadata;

/**
 * A structure containing information about a beatmap.
 */
public class BeatmapData {
    /**
     * General information about this beatmap.
     */
    public final BeatmapGeneral general = new BeatmapGeneral();

    /**
     * Information used to identify this beatmap.
     */
    public final BeatmapMetadata metadata = new BeatmapMetadata();

    /**
     * Difficulty settings of this beatmap.
     */
    public final BeatmapDifficulty difficulty = new BeatmapDifficulty();

    /**
     * Events of the beatmap.
     */
    public final BeatmapEvents events = new BeatmapEvents();

    /**
     * Raw data of timing points in this beatmap.
     */
    public final ArrayList<String> timingPoints = new ArrayList<>();

    /**
     * Raw data of hit objects in this beatmap.
     */
    public final ArrayList<String> hitObjects = new ArrayList<>();

    /**
     * The path of parent folder of this beatmap.
     */
    private String folder;

    /**
     * The format version of this beatmap.
     */
    private int formatVersion = 14;

    /**
     * Gets the path of parent folder of this beatmap.
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the path of parent folder of this beatmap.
     *
     * @param path The path of the parent folder.
     */
    public void setFolder(String path) {
        folder = path;
    }

    /**
     * Gets the format version of this beatmap.
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * Sets the format version of this beatmap.
     *
     * @param formatVersion The format version of this beatmap.
     */
    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }
}
