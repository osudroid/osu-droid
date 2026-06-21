package com.osudroid.discord

import android.content.Context
import android.content.Intent
import android.util.Log
import com.discord.socialsdk.DiscordSocialSdkInit
import com.osudroid.BuildSettings
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Manages the Discord Social SDK rich presence lifecycle.
 *
 * ## Lifecycle
 * 1. [init]: called once from [MainActivity.onLoadEngine]. Automatically calls [connect] if rich
 *    presence is already enabled in settings, so returning users reconnect without any extra tap.
 * 2. [connect]: uses a stored token if one exists (silent reconnect); only triggers the OAuth2
 *    PKCE flow on first use or after a token expires. Can also be invoked from the
 *    "Connect to Discord" button in Settings.
 * 3. [setActivity] / [clearActivity]: called at scene transitions to update what Discord shows.
 * 4. [disconnect]: called when the app is destroyed to release native resources.
 *
 * ## Threading
 * [setActivity] and [clearActivity] may be called from any thread. The underlying JNI calls are
 * safe because the SDK's `UpdateRichPresence` / `ClearRichPresence` are internally synchronized.
 * The callback loop runs on [Dispatchers.Default].
 */
object DiscordPresenceManager {
    private const val TAG = "DiscordPresenceManager"
    private const val PREFS_NAME = "discord"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private val clientId = BuildSettings.DISCORD_CLIENT_ID.toLongOrNull() ?: 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var callbackJob: Job? = null
    private val callbackDelay = 16.milliseconds

    private var isInitialized = false

    @Volatile
    var isConnected = false
        private set

    @Volatile
    private var isPendingAuthorization = false

    private var currentActivity: UserActivity = UserActivity.Idle

    private val activity
        get() = GlobalManager.getInstance().mainActivity

    /**
     * Unix epoch milliseconds captured at app launch; used as the presence elapsed-time origin.
     * Stored once so the Discord "elapsed" timer reflects time-in-app, not time-in-scene.
     */
    private var appStartTime = 0L

    /**
     * Initializes the Discord SDK. Must be called before [connect].
     *
     * If rich presence is already enabled in settings, [connect] is called automatically so that
     * returning users reconnect on launch without needing to tap "Connect to Discord" again.
     */
    @JvmStatic
    fun init(activity: MainActivity) {
        appStartTime = System.currentTimeMillis()

        if (clientId == 0L) {
            Log.w(TAG, "Discord client ID not configured, skipping initialization.")
            return
        }

        DiscordSocialSdkInit.setEngineActivity(activity)
        DiscordNative.create()
        isInitialized = true
        Log.d(TAG, "SDK initialized with client ID $clientId.")

        if (Config.isDiscordRichPresenceEnabled() && loadRefreshToken() != null) {
            connect()
        }
    }

    /**
     * Connects to Discord. If a saved refresh token exists, reconnects silently without opening
     * Discord or a browser. Otherwise, starts the OAuth2 PKCE flow (opens Discord for consent).
     */
    @JvmStatic
    fun connect() {
        if (!isInitialized) {
            Log.w(TAG, "connect() called before init().")
            return
        }

        val savedRefreshToken = loadRefreshToken()

        if (savedRefreshToken != null) {
            Log.d(TAG, "Reconnecting with stored refresh token.")
            DiscordNative.refreshTokenAndConnect(clientId, savedRefreshToken)
        } else {
            Log.d(TAG, "No saved refresh token, starting authorization flow.")
            DiscordNative.authorize(clientId)
            isPendingAuthorization = true
        }

        startCallbackLoop()
    }

    /**
     * Clears presence, stops the callback loop, and marks the client as disconnected.
     */
    @JvmStatic
    fun disconnect() {
        if (!isInitialized) {
            return
        }

        clearActivity()
        stopCallbackLoop()
        isConnected = false
        Log.d(TAG, "Disconnected from Discord.")
    }

    /**
     * Updates the user's Discord rich presence.
     *
     * @param activity The current user activity. Each subclass provides its own status and details.
     */
    @JvmStatic
    fun setActivity(activity: UserActivity) {
        currentActivity = activity

        if (!Config.isDiscordRichPresenceEnabled() || !isConnected) {
            return
        }

        val online = OnlineManager.getInstance()
        val username = online.username
        val rank = online.rank
        val largeText = if (username.isNotEmpty() && rank > 0) "$username (rank #%,d)".format(rank)
                        else username

        Log.d(TAG, "setActivity(${activity::class.simpleName}) details='${activity.details}' state='${activity.status}' party=${activity.partySize}/${activity.partyMax}")
        DiscordNative.updateRichPresence(activity.details ?: "", activity.status, activity.partySize, activity.partyMax, appStartTime, largeText)
    }

    /**
     * Refreshes the current activity.
     *
     * Call this to update data that would only be present after a certain time (e.g., online rank).
     */
    @JvmStatic
    fun refreshActivity() {
        setActivity(currentActivity)
    }

    /**
     * Clears the user's Discord rich presence. No-op if not connected.
     */
    @JvmStatic
    fun clearActivity() {
        if (!isConnected) {
            return
        }

        Log.d(TAG, "clearActivity().")
        DiscordNative.clearRichPresence()
    }

    private fun startCallbackLoop() {
        callbackJob?.cancel()
        var didReturnToGame = false
        var wasReady = false

        callbackJob = scope.launch {
            while (isActive) {
                DiscordNative.runCallbacks()

                // After authorization, the redirect URI does not bring us back to the game. This means we need to
                // manually bring the game to the front after the user authorizes in Discord. We only do this once per
                // authorization.
                if (!didReturnToGame && DiscordNative.isAuthorized()) {
                    didReturnToGame = true
                    isPendingAuthorization = false
                    bringGameToFront()
                }

                if (DiscordNative.hasAuthorizationFailed()) {
                    DiscordNative.clearAuthorizationFailed()
                    isPendingAuthorization = false
                    Log.w(TAG, "Authorization cancelled or rejected by user, stopping.")
                    stopCallbackLoop()
                    return@launch
                }

                if (DiscordNative.needsReauth()) {
                    DiscordNative.clearNeedsReauth()
                    didReturnToGame = false
                    clearSavedRefreshToken()
                    Log.w(TAG, "Stored refresh token rejected, re-authorizing.")
                    DiscordNative.authorize(clientId)
                }

                if (DiscordNative.hasNewRefreshToken()) {
                    saveRefreshToken(DiscordNative.getRefreshToken())
                    DiscordNative.clearNewRefreshTokenFlag()
                }

                val isNowReady = DiscordNative.isReady()

                if (isNowReady && !wasReady) {
                    isConnected = true
                    Log.d(TAG, "Discord ready.")
                    refreshActivity()
                } else if (!isNowReady && wasReady) {
                    isConnected = false
                    Log.d(TAG, "Discord disconnected.")
                }

                wasReady = isNowReady

                delay(callbackDelay)
            }
        }
    }

    /**
     * Called from [MainActivity.onResume]. If the user returned to the game while an OAuth
     * authorization was still pending (e.g. they pressed back in Discord without authorizing),
     * this aborts the flow so the SDK stops trying to re-open Discord on every callback tick.
     * The abort fires the [DiscordNative.hasAuthorizationFailed] flag, which the callback loop
     * handles on the next tick.
     */
    fun onActivityResume() {
        if (isPendingAuthorization) {
            Log.d(TAG, "onActivityResume: aborting pending authorization.")
            DiscordNative.abortAuthorize()
        }
    }

    private fun bringGameToFront() {
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        activity.startActivity(intent)
        Log.d(TAG, "bringGameToFront: returning to game after OAuth")
    }

    private fun stopCallbackLoop() {
        callbackJob?.cancel()
        callbackJob = null
    }

    private fun saveRefreshToken(refreshToken: String) {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()

        Log.d(TAG, "Refresh token saved.")
    }

    private fun loadRefreshToken(): String? =
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH_TOKEN, null)

    private fun clearSavedRefreshToken() {
        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
