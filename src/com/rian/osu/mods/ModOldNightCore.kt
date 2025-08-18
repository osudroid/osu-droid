package com.rian.osu.mods

/**
 * Represents the "old" [ModNightCore].
 *
 * This [Mod] is used solely for difficulty calculation of replays with version 3 or older. The reason behind this is a
 * bug that was patched in replay version 4, where all audio that did not have 44100Hz frequency would slow down or
 * speed up depending on the frequency of the audio.
 *
 * The equation for the playback rate with respect to the audio frequency (in Hz) was:
 *
 * ```
 * playback_rate = 44100 * 1.5 / audio_frequency
 * ```
 *
 * For example, if the audio's frequency is 48000Hz, the audio would play at `44100 * 1.5 / 48000 = 1.378125` playback
 * rate.
 *
 * This [Mod] assumes that the audio frequency is 48000Hz and applies the same equation to calculate the playback rate.
 * The frequency was chosen after sampling many audio files that were affected by this bug, and it seemed that 48000Hz
 * was the most common frequency used in those files.
 *
 * Realistically, it is possible to obtain the audio frequency during gameplay loading (and therefore would result in
 * the correct playback rate), but this would require additional work and the current solution is deemed sufficient for
 * the purpose it serves.
 */
class ModOldNightCore : ModNightCore() {
    init { trackRateMultiplier = 44.1f * 1.5f / 48 }

    // Force the score multiplier to be 1.12x, as it was the value used in the old versions (due to 1.5x rate).
    override val scoreMultiplier = 1.12f
}