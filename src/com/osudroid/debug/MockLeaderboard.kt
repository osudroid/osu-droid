package com.osudroid.debug

import com.osudroid.online.model.BeatmapScoreModel
import kotlin.random.Random

/**
 * Generates a fake global leaderboard for debug builds, so the song select leaderboard can be tested
 * without an online account. See [com.osudroid.BuildSettings.MOCK_LEADERBOARD].
 *
 * The data is deterministic per beatmap hash: the same beatmap always shows the same scores.
 */
object MockLeaderboard {

    private val playerNames = listOf(
        "Rafis", "Cookiezi", "WhiteCat", "Mrekk", "Vaxei", "Aetrna", "Lifeline", "Chicony", "Jakads",
        "Ryuk", "Akolibed", "Bocchi", "Nyanko", "SpeedyBoi", "StreamGod", "AimLord", "HDHRenjoyer",
        "CasualClicker", "TapTapper", "SliderLover", "SpinToWin", "JumpKing", "TechWizard", "Rhythmia",
        "OsuDroidPro", "MobileMaster", "ThumbGod", "TouchScreenHero", "NotesHitter", "AccuracyFan"
    )

    private val modCombinations = listOf(
        emptyList(),
        listOf("HD"),
        listOf("HR"),
        listOf("HD", "HR"),
        listOf("DT"),
        listOf("HD", "DT"),
        listOf("HD", "HR", "DT"),
        listOf("NF"),
        listOf("EZ", "HD"),
        listOf("NC", "HD")
    )

    /**
     * The name used for the personal best entry.
     */
    private const val PERSONAL_BEST_NAME = "You (mock)"


    /**
     * @param hash The beatmap MD5, used as the random seed.
     * @param rankByPP Whether entries are ranked by pp instead of score.
     */
    fun generate(hash: String, rankByPP: Boolean): List<BeatmapScoreModel> {
        val random = Random(hash.hashCode())
        val entryCount = 15 + random.nextInt(36)
        val maxCombo = 300 + random.nextInt(1700)

        val entries = (0 until entryCount).map { i ->
            val accuracy = (1f - random.nextFloat() * 0.12f).coerceAtMost(1f)
            val combo = (maxCombo * (0.6f + random.nextFloat() * 0.4f)).toInt()

            BeatmapScoreModel(
                scoreId = 1_000_000L + i,
                rank = 0,
                mark = markFor(accuracy, combo == maxCombo),
                playerName = playerNames[(i + random.nextInt(playerNames.size)) % playerNames.size] + (if (i >= playerNames.size) "_$i" else ""),
                mods = serialize(modCombinations[random.nextInt(modCombinations.size)]),
                score = (combo.toLong() * 6000L * accuracy).toLong(),
                pp = 50f + random.nextFloat() * 450f * accuracy,
                maxCombo = combo,
                accuracy = accuracy,
            )
        }

        val sorted = entries
            .sortedByDescending { if (rankByPP) it.pp.toDouble() else it.score.toDouble() }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }

        // A personal best outside the leaderboard, docked below the list.
        val personalBestAccuracy = 0.85f + random.nextFloat() * 0.1f
        val personalBest = BeatmapScoreModel(
            scoreId = 999L,
            rank = entryCount + 1 + random.nextInt(200),
            mark = markFor(personalBestAccuracy, false),
            playerName = PERSONAL_BEST_NAME,
            mods = serialize(listOf("HD")),
            score = (maxCombo * 0.5f * 6000f * personalBestAccuracy).toLong(),
            pp = 30f + random.nextFloat() * 40f,
            maxCombo = (maxCombo * 0.5f).toInt(),
            accuracy = personalBestAccuracy,
            isPersonalBest = true,
            isInLeaderboard = false,
        )

        return sorted + personalBest
    }


    private fun markFor(accuracy: Float, isFullCombo: Boolean) = when {
        accuracy >= 1f -> "X"
        accuracy >= 0.95f && isFullCombo -> "S"
        accuracy >= 0.93f -> "A"
        accuracy >= 0.85f -> "B"
        accuracy >= 0.8f -> "C"
        else -> "D"
    }

    private fun serialize(acronyms: List<String>) =
        acronyms.joinToString(prefix = "[", postfix = "]") { "{\"acronym\":\"$it\"}" }

}
