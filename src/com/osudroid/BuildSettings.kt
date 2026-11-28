package com.osudroid

import ru.nsu.ccfit.zuev.osuplus.BuildConfig

object BuildSettings {

    /**
     * Whether to use textures or not.
     */
    const val NO_TEXTURES_MODE = false

    /**
     * Whether to show entity boundaries or not.
     */
    const val SHOW_ENTITY_BOUNDARIES = false

    /**
     * Whether to use fake multiplayer mode or not.
     */
    val MOCK_MULTIPLAYER = BuildConfig.DEBUG

    /**
     * Whether to use the debug playground scene or not.
     */
    const val DEBUG_PLAYGROUND = false

    /**
     * Delays every Nth frame on the GL thread in debug builds to test the FPS counter, or 0 to turn it off.
     */
    const val DEBUG_FRAME_DELAY_INTERVAL = 0

    /**
     * How long to delay a frame in milliseconds when [DEBUG_FRAME_DELAY_INTERVAL] is enabled.
     */
    const val DEBUG_FRAME_DELAY_MS = 12

    /**
     * The signature to use when skipping hardware attestation.
     */
    @JvmStatic
    val DEBUG_ATTESTATION_SIGN = ""

    /**
     * Whether to skip hardware attestation.
     */
    @JvmStatic
    val DEBUG_SKIP_ATTESTATION = BuildConfig.DEBUG && DEBUG_ATTESTATION_SIGN.isNotEmpty()
}
