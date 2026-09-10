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
     * Discord application client ID. Set to 0 to disable Discord integration.
     */
    const val DISCORD_CLIENT_ID = "1517948535000137988"

    /**
     * Whether to use the debug playground scene or not.
     */
    const val DEBUG_PLAYGROUND = false
}


