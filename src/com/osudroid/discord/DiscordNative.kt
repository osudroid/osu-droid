package com.osudroid.discord

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative

/**
 * JNI bridge to Discord's Social SDK.
 *
 * The native library must be initialized in order ([create] --> [authorize] or
 * [refreshTokenAndConnect] --> [runCallbacks] loop --> [updateRichPresence] /
 * [clearRichPresence] --> [destroy]) and is not thread-safe except where noted.
 *
 * All calls should originate from [DiscordPresenceManager].
 */
internal object DiscordNative {
    init { System.loadLibrary("discord_jni") }

    /**
     * Allocates the `discordpp::Client` and registers the status-changed callback.
     */
    @JvmStatic
    external fun create()

    /**
     * Returns `true` once the client status reaches `Ready` (fully authenticated and connected).
     */
    @JvmStatic
    @CriticalNative
    external fun isReady(): Boolean

    /**
     * Returns `true` after [authorize]'s `GetToken` callback succeeds, regardless of connection
     * status. Used to detect OAuth completion so the game can be brought back to the foreground.
     */
    @JvmStatic
    @CriticalNative
    external fun isAuthorized(): Boolean

    /**
     * Starts the OAuth2 PKCE flow: opens Discord (or a browser) for user consent, then exchanges
     * the code for a token via `GetToken` --> `UpdateToken` --> `Connect`.
     *
     * Call this only when no saved refresh token is available. On success, [hasNewRefreshToken]
     * becomes `true` and the token can be read via [getRefreshToken] and persisted.
     */
    @JvmStatic
    external fun authorize(clientId: Long)

    /**
     * Silently reconnects using a previously persisted refresh token, skipping OAuth consent.
     * Calls `RefreshToken` --> `UpdateToken` --> `Connect`.
     *
     * The old refresh token is invalidated and [hasNewRefreshToken] becomes `true` with the replacement token
     * to persist. If the token is rejected, [needsReauth] becomes `true` and [authorize] must be called.
     */
    @JvmStatic
    external fun refreshTokenAndConnect(clientId: Long, refreshToken: String)

    /**
     * Returns `true` when a new refresh token is ready to be persisted, either after the initial
     * [authorize] flow or after a [refreshTokenAndConnect] call rotates the token. Clear the flag
     * with [clearNewRefreshTokenFlag] after saving.
     */
    @JvmStatic
    @CriticalNative
    external fun hasNewRefreshToken(): Boolean

    /**
     * Returns the latest refresh token. Only valid when [hasNewRefreshToken] is `true`.
     */
    @JvmStatic
    @FastNative
    external fun getRefreshToken(): String

    /**
     * Clears the [hasNewRefreshToken] flag after the token has been saved.
     */
    @JvmStatic
    @CriticalNative
    external fun clearNewRefreshTokenFlag()

    /**
     * Returns `true` when [refreshTokenAndConnect] fails, meaning the stored token is stale.
     * On detecting this, the caller should clear the stored token and call [authorize].
     */
    @JvmStatic
    @CriticalNative
    external fun needsReauth(): Boolean

    /**
     * Clears the [needsReauth] flag after the caller has initiated re-authorization.
     */
    @JvmStatic
    @CriticalNative
    external fun clearNeedsReauth()

    /**
     * Returns `true` when the user canceled or rejected the OAuth authorization prompt.
     * On detecting this, the caller should stop the callback loop and let the user retry manually.
     */
    @JvmStatic
    @CriticalNative
    external fun hasAuthorizationFailed(): Boolean

    /**
     * Clears the [hasAuthorizationFailed] flag after the caller has handled the cancellation.
     */
    @JvmStatic
    @CriticalNative
    external fun clearAuthorizationFailed()

    /**
     * Cancels an in-progress [authorize] call. Causes the [authorize] callback to fire with an
     * `Aborted` error, which sets [hasAuthorizationFailed]. No-op if no authorization is pending.
     */
    @JvmStatic
    external fun abortAuthorize()

    /**
     * Pumps the SDK event loop. Must be called repeatedly for callbacks to fire.
     */
    @JvmStatic
    external fun runCallbacks()

    /**
     * Sets the user's Discord rich presence.
     *
     * @param details Primary line shown under the application name (e.g. beatmap title).
     * @param state Secondary line shown below [details] (e.g. "Playing", "In a multiplayer room").
     * @param partySize Current number of players. Pass `0` to omit the party field entirely.
     * @param partyMax Maximum number of players in the party.
     * @param startTimestamp Unix epoch milliseconds for the "elapsed" timer. Pass `0` to omit.
     * @param largeText Tooltip text shown when hovering the large image (e.g. "username (#rank)").
     *   Pass an empty string to omit.
     */
    @JvmStatic
    external fun updateRichPresence(details: String, state: String, partySize: Int, partyMax: Int, startTimestamp: Long, largeText: String)

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
