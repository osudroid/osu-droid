package com.osudroid.beatmaplisting

/**
 * A link to a beatmap or beatmap set, in the `osu://` scheme.
 */
sealed interface BeatmapLink {
    /**
     * A link to a beatmap, identified by its beatmap ID (`osu://b/{id}`).
     */
    data class Beatmap(@JvmField val beatmapId: Long) : BeatmapLink

    /**
     * A link to a beatmapset.
     */
    data class BeatmapSet(@JvmField val beatmapSetId: Long) : BeatmapLink

    companion object {
        private val LINK_REGEX = Regex("^osu://(b|s|dl)/(\\d+)(?:[/?#].*)?$", RegexOption.IGNORE_CASE)

        /**
         * Parses an `osu://` beatmap link.
         *
         * Anything after the ID (a trailing `/`, a query string or a fragment) is ignored. The scheme and the
         * link type are matched case-insensitively.
         *
         * @param uri The link to parse.
         * @return The parsed link, or `null` if [uri] is not a valid beatmap link.
         */
        @JvmStatic
        fun parse(uri: String): BeatmapLink? {
            val match = LINK_REGEX.matchEntire(uri) ?: return null
            val (type, idText) = match.destructured

            val id = idText.toLongOrNull()?.takeIf { it > 0 } ?: return null

            return when (type.lowercase()) {
                "b" -> Beatmap(id)
                "s", "dl" -> BeatmapSet(id)
                else -> null
            }
        }
    }
}
