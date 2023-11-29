package ru.nsu.ccfit.zuev.osu.beatmap.sections;

/**
 * Contains information used to identify a beatmap.
 */
public class BeatmapMetadata {

    /**
     * The romanized song title of this beatmap.
     */
    public String title = "";

    /**
     * The song title of this beatmap.
     */
    public String titleUnicode = "";

    /**
     * The romanized artist of the song of this beatmap.
     */
    public String artist = "";

    /**
     * The song artist of this beatmap.
     */
    public String artistUnicode = "";

    /**
     * The creator of this beatmap.
     */
    public String creator = "";

    /**
     * The difficulty name of this beatmap.
     */
    public String version = "";

    /**
     * The original media the song was produced for.
     */
    public String source = "";

    /**
     * The search terms of this beatmap.
     */
    public String tags = "";

    /**
     * The ID of this beatmap.
     */
    public int beatmapID = -1;

    /**
     * The ID of this beatmap set containing this beatmap.
     */
    public int beatmapSetID = -1;

    public BeatmapMetadata() {
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapMetadata(BeatmapMetadata source) {
        title = source.title;
        titleUnicode = source.titleUnicode;
        artist = source.artist;
        artistUnicode = source.artistUnicode;
        creator = source.creator;
        version = source.version;
        this.source = source.source;
        tags = source.tags;
        beatmapID = source.beatmapID;
        beatmapSetID = source.beatmapSetID;
    }

    /**
     * Deep clones this instance.
     *
     * @return The deep cloned instance.
     */
    public BeatmapMetadata deepClone() {
        return new BeatmapMetadata(this);
    }

}
