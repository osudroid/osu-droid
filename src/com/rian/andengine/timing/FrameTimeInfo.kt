package com.rian.andengine.timing

data class FrameTimeInfo(
    /**
     * Elapsed time during last frame in milliseconds.
     */
    var elapsed: Double = 0.0,

    /**
     * Begin time of this frame.
     */
    var current: Double = 0.0
)