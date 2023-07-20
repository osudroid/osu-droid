package com.reco1l.api.ibancho.data

data class LiveScore
(
        val uid: Long,
        val score: Int,
        val combo: Int,
        val accuracy: Int
)
{
    override fun toString() = """
        {
            "score": $score,
            combo: $combo,
            accuracy: $accuracy,
        }
    """.trimIndent()
}
