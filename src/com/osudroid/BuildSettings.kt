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
     * Whether to show a generated global leaderboard when not logged in, see [com.osudroid.debug.MockLeaderboard].
     */
    val MOCK_LEADERBOARD = BuildConfig.DEBUG

    /**
     * Whether to use the debug playground scene or not.
     */
    const val DEBUG_PLAYGROUND = false
}


