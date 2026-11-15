package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo

/**
 * A search text, split into words that all have to match.
 *
 * A word is searched in the texts of a beatmap, unless it compares a difficulty value like `ar>9` or `star<=5.5`.
 */
class BeatmapSearchQuery(text: String) {

    private val words = ArrayList<String>()
    private val criteria = ArrayList<Criterion>()


    init {
        for (word in text.lowercase().split(' ')) {
            if (word.isBlank()) {
                continue
            }

            val match = CRITERION.matchEntire(word)

            if (match != null) {
                criteria += Criterion(match.groupValues[1], match.groupValues[2], match.groupValues[3].toFloat())
            } else {
                words += word
            }
        }
    }


    /**
     * Whether every word of the search matches at least one of the [beatmaps].
     */
    fun matches(beatmaps: List<BeatmapInfo>) =
        words.all { word -> beatmaps.any { it.contains(word) } } && criteria.all { criterion -> beatmaps.any(criterion::matches) }

    private fun BeatmapInfo.contains(word: String) =
        title.contains(word, true)
            || titleUnicode.contains(word, true)
            || artist.contains(word, true)
            || artistUnicode.contains(word, true)
            || creator.contains(word, true)
            || version.contains(word, true)
            || tags.contains(word, true)
            || source.contains(word, true)
            || id?.toString() == word
            || setId?.toString() == word


    private class Criterion(private val key: String, private val operator: String, private val value: Float) {

        fun matches(beatmapInfo: BeatmapInfo): Boolean {
            val actual = when (key) {
                "ar" -> beatmapInfo.approachRate
                "od" -> beatmapInfo.overallDifficulty
                "cs" -> beatmapInfo.circleSize
                "hp" -> beatmapInfo.hpDrainRate
                // Of the current difficulty algorithm, like the star ratings on the panels.
                else -> beatmapInfo.getStarRating()
            }

            return when (operator) {
                "=" -> actual == value
                "<" -> actual < value
                ">" -> actual > value
                "<=" -> actual <= value
                else -> actual >= value
            }
        }
    }


    companion object {
        private val CRITERION = Regex("(ar|od|cs|hp|star)(<=|>=|=|<|>)(\\d+(?:\\.\\d+)?)")
    }

}
