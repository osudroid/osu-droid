package com.osudroid

import ru.nsu.ccfit.zuev.osu.Config
import kotlin.math.min

/**
 * How the render surface, the engine and the update clock are configured for a frame limiter setting.
 */
data class FrameLimiterMode(
    /** Whether buffer swaps wait for the display's vertical sync. */
    val vsync: Boolean,
    /** Whether every frame carries an explicit presentation time. */
    val presentationTime: Boolean,
    /** Whether a Choreographer callback paces the render loop instead of the swap interval. */
    val choreographerPacing: Boolean,
    /** Whether the next update only starts once the frame has been swapped. */
    val releaseUpdateAfterSwap: Boolean,
    /** The update rate cap of the update clock. */
    val maximumUpdateHz: Float
) {
    companion object {
        /**
         * The maximum limit of the update rate, regardless of the frame limiter setting.
         */
        const val MAXIMUM_UPDATE_HZ = 1000f

        /**
         * The mode for a frame limiter setting.
         *
         * @param frameLimiter A value returned by [Config.getFrameLimiter].
         * @param refreshRate The current refresh rate of the display, which the refresh rate multiples scale with.
         */
        @JvmStatic
        fun of(frameLimiter: Int, refreshRate: Float): FrameLimiterMode {
            val lowLatency = frameLimiter == Config.FRAME_LIMITER_VSYNC_LOW_LATENCY
            val vsync = lowLatency || frameLimiter == Config.FRAME_LIMITER_VSYNC

            // With vertical sync on, the display paces the loop and the hard limit is never reached. Without it,
            // the clock is the only thing pacing the loop.
            val maximumUpdateHz =
                if (vsync || frameLimiter == Config.FRAME_LIMITER_UNLIMITED) MAXIMUM_UPDATE_HZ
                else min(refreshRate * frameLimiter, MAXIMUM_UPDATE_HZ)

            return FrameLimiterMode(
                vsync = vsync,
                // Every mode but plain vertical sync gives each frame a presentation time.
                presentationTime = frameLimiter != Config.FRAME_LIMITER_VSYNC,
                choreographerPacing = lowLatency,
                // The next update only starts after the vsync wait, so it samples input as late as possible.
                releaseUpdateAfterSwap = lowLatency,
                maximumUpdateHz = maximumUpdateHz
            )
        }
    }
}
