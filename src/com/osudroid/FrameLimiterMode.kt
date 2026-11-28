package com.osudroid

import androidx.annotation.StringRes
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * How rendering is paced.
 */
enum class FrameLimiterMode(
    /**
     * The value stored in preferences.
     */
    @JvmField
    val value: Int,

    /**
     * The name shown in settings.
     */
    @JvmField
    @field:StringRes
    val nameResId: Int
) {
    /**
     * Rendering is paced by the display's vertical sync.
     */
    VSync(1, R.string.opt_frame_limiter_vsync),

    /**
     * Like [VSync], but each frame starts as late as possible, trading throughput for lower input-to-display latency.
     */
    VSyncLowLatency(-1, R.string.opt_frame_limiter_vsync_low_latency),

    /**
     * Vertical sync is off and updates are capped at 2x the display's refresh rate.
     */
    RefreshRate2x(2, R.string.opt_frame_limiter_refresh_rate_2x),

    /**
     * Vertical sync is off and updates are capped at 4x the display's refresh rate.
     */
    RefreshRate4x(4, R.string.opt_frame_limiter_refresh_rate_4x),

    /**
     * Vertical sync is off and updates are capped at 8x the display's refresh rate.
     */
    RefreshRate8x(8, R.string.opt_frame_limiter_refresh_rate_8x),

    /**
     * Vertical sync is off and updates are only capped by the update clock's limit.
     */
    Unlimited(0, R.string.opt_frame_limiter_unlimited);

    /**
     * Whether the display's vertical sync paces rendering.
     */
    val isVSync get() = this == VSync || this == VSyncLowLatency

    /**
     * The multiple of the refresh rate that updates are capped at, or 0 if the refresh rate does not cap them.
     */
    val refreshRateMultiple get() = if (value > 1) value else 0

    companion object {
        /**
         * Parses a stored value, falling back to [VSync] for anything unknown.
         */
        @JvmStatic
        fun parse(value: String?) = entries.find { it.value.toString() == value } ?: VSync
    }
}
