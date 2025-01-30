package com.rian.osu.beatmap

/**
 * An empty [HitWindow] that does not have any hit windows.
 *
 * No time values are provided (meaning instantaneous hit or miss).
 */
class EmptyHitWindow : HitWindow() {
    override val greatWindow = 0f
    override val okWindow = 0f
    override val mehWindow = 0f
}