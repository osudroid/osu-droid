package com.osudroid.beatmaps

/**
 * An empty [HitWindow] that does not have any hit windows.
 *
 * No time values are provided (meaning instantaneous hit or miss).
 */
class EmptyHitWindow : HitWindow(null) {
    override val greatWindow = 0.0
    override val okWindow = 0.0
    override val mehWindow = 0.0
}