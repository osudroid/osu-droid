package main.osu.beatmap.sections;

/**
 * Contains information used to identify a beatmap.
 */
public class BeatmapMetadata {
    /**
     * The romanized song title of the beatmap.
     */
    public String title = "";

    /**
     * The song title of the beatmap.
     */
    public String titleUnicode = "";

    /**
     * The romanized artist of the song of the beatmap.
     */
    public String artist = "";

    /**
     * The song artist of the beatmap.
     */
    public String artistUnicode = "";

    /**
     * The creator of the beatmap.
     */
    public String creator = "";

    /**
     * The difficulty name of the beatmap.
     */
    public String version = "";

    /**
     * The original media the song was produced for.
     */
    public String source = "";

    /**
     * The search terms of the beatmap.
     */
    public String tags = "";
}
