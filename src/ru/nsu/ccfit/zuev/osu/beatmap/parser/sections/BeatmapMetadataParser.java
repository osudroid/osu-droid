package ru.nsu.ccfit.zuev.osu.beatmap.parser.sections;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's metadata section.
 */
public class BeatmapMetadataParser extends BeatmapKeyValueSectionParser {
    @Override
    public void parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "Title":
                data.metadata.title = p[1];
                break;
            case "TitleUnicode":
                data.metadata.titleUnicode = p[1];
                break;
            case "Artist":
                data.metadata.artist = p[1];
                break;
            case "ArtistUnicode":
                data.metadata.artistUnicode = p[1];
                break;
            case "Creator":
                data.metadata.creator = p[1];
                break;
            case "Version":
                data.metadata.version = p[1];
                break;
            case "Source":
                data.metadata.source = p[1];
                break;
            case "Tags":
                data.metadata.tags = p[1];
                break;
            case "BeatmapID":
                data.metadata.beatmapID = parseInt(p[1]);
                break;
            case "BeatmapSetID":
                data.metadata.beatmapSetID = parseInt(p[1]);
                break;
        }
    }
}
