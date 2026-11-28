package com.osudroid.ui

import ru.nsu.ccfit.zuev.osu.Config

/**
 * Decisions made by the [FPSCounter].
 */
internal object FPSCounterRules {
    /**
     * The frame time, in seconds, above which a frame counts as a spike.
     */
    const val SPIKE_TIME = 0.02f

    /**
     * The fraction of the target rate below which the displayed rate counts as significantly low.
     */
    const val SIGNIFICANT_RATIO = 0.8f

    /**
     * Whether the counter measures presented frames rather than updates for a frame limiter setting.
     *
     * @param frameLimiter A value returned by [Config.getFrameLimiter].
     */
    fun usesPresentedFrames(frameLimiter: Int) =
        frameLimiter == Config.FRAME_LIMITER_VSYNC || frameLimiter == Config.FRAME_LIMITER_VSYNC_LOW_LATENCY

    /**
     * Whether the counter reads the presented clock rather than the update clock.
     *
     * @param shouldTrack Whether presented frames are being measured.
     * @param isAvailable Whether the presented clock receives frame timestamps.
     * @param presentedFramesPerSecond The frame rate the presented clock reports.
     */
    fun usesPresentedClock(shouldTrack: Boolean, isAvailable: Boolean, presentedFramesPerSecond: Float) =
        shouldTrack && isAvailable && presentedFramesPerSecond > 0f

    /**
     * Whether an update took long enough to show right away, given a display that was steady until now.
     *
     * @param displayedFrameTime The displayed frame time, in seconds.
     * @param elapsedFrameTime The time the last update took, in seconds.
     */
    fun isFrameTimeSpike(displayedFrameTime: Float, elapsedFrameTime: Float) =
        displayedFrameTime < SPIKE_TIME && elapsedFrameTime > SPIKE_TIME

    /**
     * Whether a gap between presented frames was long enough to show right away, given a display that was steady
     * until now.
     *
     * @param displayedFps The displayed frame rate.
     * @param longestGap The longest gap between presented frames since the last update, in seconds.
     */
    fun isFpsSpike(displayedFps: Float, longestGap: Float) =
        displayedFps > 1f / SPIKE_TIME && longestGap > SPIKE_TIME

    /**
     * Whether either the displayed frame rate or the displayed frame time is below [SIGNIFICANT_RATIO] of the target.
     *
     * @param displayedFps The displayed frame rate.
     * @param displayedFrameTime The displayed frame time, in seconds.
     * @param aimFps The target frame rate.
     */
    fun isBelowTarget(displayedFps: Float, displayedFrameTime: Float, aimFps: Float): Boolean {
        val minimumFps = aimFps * SIGNIFICANT_RATIO

        return displayedFps < minimumFps || (displayedFrameTime > 0f && 1f / displayedFrameTime < minimumFps)
    }
}
