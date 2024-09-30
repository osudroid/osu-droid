package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import kotlinx.coroutines.CoroutineScope

/**
 * A parser for parsing a beatmap's metadata section.
 */
object BeatmapMetadataParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = splitProperty(line, scope)?.let {
        when (it.first) {
            "Title" -> beatmap.metadata.title = it.second
            "TitleUnicode" -> beatmap.metadata.titleUnicode = it.second
            "Artist" -> beatmap.metadata.artist = it.second
            "ArtistUnicode" -> beatmap.metadata.artistUnicode = it.second
            "Creator" -> beatmap.metadata.creator = it.second
            "Version" -> beatmap.metadata.version = it.second
            "Source" -> beatmap.metadata.source = it.second
            "Tags" -> beatmap.metadata.tags = it.second
            "BeatmapID" -> beatmap.metadata.beatmapId = parseInt(it.second)
            "BeatmapSetID" -> beatmap.metadata.beatmapSetId = parseInt(it.second)
        }
    } ?: throw UnsupportedOperationException("Malformed metadata property: $line")
}
