package com.rian.andengine.timing

data class FrameTimeInfo(
    /**
     * Elapsed time during last frame in seconds.
     */
    var elapsed: Float = 0f,

    /**
     * Begin time of this frame in seconds.
     */
    var current: Float = 0f
)