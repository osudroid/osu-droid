package com.rian.andengine.timing

/**
 * An [IClock] which will only update its current time when a frame process is triggered. Useful for keeping a
 * consistent time state across an individual update.
 */
interface IFrameBasedClock : IClock {
    /**
     * Elapsed time since last frame in milliseconds.
     */
    val elapsedFrameTime: Double

    /**
     * A moving average representation of the frames per second of this [IFrameBasedClock].
     *
     * Do not use this for any timing purposes (use [elapsedFrameTime] instead).
     */
    val framesPerSecond: Double

    /**
     * Processes one frame. Generally should be run once per update loop.
     */
    fun processFrame()

    /**
     * The current frame's time.
     */
    val timeInfo: FrameTimeInfo
}