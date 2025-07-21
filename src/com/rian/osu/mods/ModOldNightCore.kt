package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the "old" [ModNightCore].
 *
 * This mod is used solely for difficulty calculation of replays with version 3 or older. The reason behind this is a
 * bug that was patched in replay version 4, where all audio that did not have 44100Hz frequency would slow down.
 *
 * After some testing, it was discovered that such replays were played at 1.39x speed instead of 1.5x, which is
 * represented by this mod.
 */
class ModOldNightCore : ModNightCore() {
    init { trackRateMultiplier = 1.39f }

    // Force the score multiplier to be 1.12x, as it was the value used in the old versions (due to 1.5x rate).
    override val scoreMultiplier = 1.12f
}