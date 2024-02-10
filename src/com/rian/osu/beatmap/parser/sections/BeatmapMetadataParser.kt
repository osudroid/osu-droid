package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap

/**
 * A parser for parsing a beatmap's metadata section.
 */
object BeatmapMetadataParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line).let {
        when (it[0]) {
            "Title" -> beatmap.metadata.title = it[1]
            "TitleUnicode" -> beatmap.metadata.titleUnicode = it[1]
            "Artist" -> beatmap.metadata.artist = it[1]
            "ArtistUnicode" -> beatmap.metadata.artistUnicode = it[1]
            "Creator" -> beatmap.metadata.creator = it[1]
            "Version" -> beatmap.metadata.version = it[1]
            "Source" -> beatmap.metadata.source = it[1]
            "Tags" -> beatmap.metadata.tags = it[1]
            "BeatmapID" -> beatmap.metadata.beatmapID = parseInt(it[1])
            "BeatmapSetID" -> beatmap.metadata.beatmapSetID = parseInt(it[1])
        }
    }
}
