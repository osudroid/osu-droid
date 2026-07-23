package com.osudroid.discord

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative

/**
 * JNI bridge to Discord's Social SDK.
 *
 * The native library must be initialized in order ([create] --> [authorize] -->
 * [runCallbacks] loop --> [provideTokens] --> [updateRichPresence] / [clearRichPresence] -->
 * [destroy]) and is not thread-safe except where noted.
 *
 * All calls should originate from [DiscordPresenceManager].
 */
internal object DiscordNative {
    init {
        System.loadLibrary("discord_jni")
    }

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
     * Starts the OAuth2 PKCE flow. This opens Discord (or a browser) for user consent.
     *
     * On success, [hasAuthorizationCode] becomes `true` and the code and verifier can be read via
     * [getAuthorizationCode] and [getVerifier] for server-side token exchange.
     */
    @JvmStatic
    external fun authorize(clientId: Long)

    /**
     * Returns `true` when the authorization code is ready for server-side exchange.
     * Clear the flag with [clearAuthorizationCode] after reading the code and verifier.
     */
    @JvmStatic
    @CriticalNative
    external fun hasAuthorizationCode(): Boolean

    /**
     * Returns the authorization code from the [authorize] callback. Only valid when [hasAuthorizationCode] is `true`.
     */
    @JvmStatic
    @FastNative
    external fun getAuthorizationCode(): String

    /**
     * Returns the PKCE verifier generated during [authorize]. Only valid when [hasAuthorizationCode] is `true`.
     */
    @JvmStatic
    @FastNative
    external fun getVerifier(): String

    /**
     * Returns the redirect URI used in the [authorize] callback. Only valid when
     * [hasAuthorizationCode] is `true`. Must be forwarded to the server verbatim so Discord can
     * validate it against the original authorization request.
     */
    @JvmStatic
    @FastNative
    external fun getRedirectUri(): String

    /**
     * Clears the [hasAuthorizationCode] flag and the pending code and verifier strings after the
     * server-side exchange has been initiated.
     */
    @JvmStatic
    @CriticalNative
    external fun clearAuthorizationCode()

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
     * Called after a successful server-side token exchange. Feeds the access token into
     * `UpdateToken` and `Connect` to complete authentication.
     */
    @JvmStatic
    external fun provideTokens(accessToken: String)

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
