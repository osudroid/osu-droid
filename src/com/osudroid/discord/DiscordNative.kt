package com.osudroid.discord

/**
 * JNI bridge to Discord's Social SDK, using its unauthenticated Rich Presence RPC path
 * (`Client::SetApplicationId` + `Client::UpdateRichPresence`, without `Client::Connect`). This
 * talks directly to a locally running Discord client -- no OAuth, no tokens, no backend
 * connection.
 *
 * The native library must be initialized in order ([create] --> [runCallbacks] loop -->
 * [updateRichPresence] / [clearRichPresence] --> [destroy]) and is not thread-safe except
 * where noted.
 *
 * All calls should originate from [DiscordPresenceManager].
 */
internal object DiscordNative {
    init {
        System.loadLibrary("discord_jni")
    }

    /**
     * Allocates the `discordpp::Client` and sets its application ID.
     */
    @JvmStatic
    external fun create(clientId: Long)

    /**
     * Pumps the SDK event loop. Must be called repeatedly for RPC callbacks to fire.
     */
    @JvmStatic
    external fun runCallbacks()

    /**
     * Sets the user's Discord rich presence via RPC to a locally running Discord client.
     * Silently does nothing if Discord isn't installed or running.
     *
     * @param details Primary line shown under the application name (e.g. beatmap title).
     * @param state Secondary line shown below [details] (e.g. "Playing", "In a multiplayer room").
     * @param partySize Current number of players. Pass `0` to omit the party field entirely.
     * @param partyMax Maximum number of players in the party.
     * @param startTimestamp Unix epoch milliseconds for the "elapsed" timer. Pass `0` to omit.
     * @param largeText Tooltip text shown when hovering the large image (e.g. "username (#rank)").
     *   Pass an empty string to omit.
     * @param buttonLabel Label for the action button (max 32 chars). Pass an empty string to omit.
     * @param buttonUrl URL opened when the button is clicked. Pass an empty string to omit.
     */
    @JvmStatic
    external fun updateRichPresence(
        details: String,
        state: String,
        partySize: Int,
        partyMax: Int,
        startTimestamp: Long,
        largeText: String,
        buttonLabel: String,
        buttonUrl: String
    )

    /**
     * Clears the user's Discord rich presence.
     */
    @JvmStatic
    external fun clearRichPresence()

    /**
     * Destroys the `discordpp::Client` and resets all native state.
     */
    @JvmStatic
    external fun destroy()
}
