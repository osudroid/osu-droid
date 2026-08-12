package com.osudroid.game

import com.rian.andengine.timing.IAdjustableClock
import com.rian.andengine.timing.IFrameBasedClock
import com.rian.andengine.timing.ISourceChangeableClock

/**
 * An interface for clocks that can be used for beatmap timing in gameplay. Applies user and global offsets.
 */
interface IGameplayClock : IFrameBasedClock, IAdjustableClock, ISourceChangeableClock {
    /**
     * The offset applied globally to all beatmaps, set via settings.
     */
    var userGlobalOffset: Float

    /**
     * The offset applied to the current beatmap, set via beatmap options.
     */
    var userBeatmapOffset: Float

    /**
     * The sum of applied offsets (global + beatmap).
     */
    val totalAppliedOffset: Float
}