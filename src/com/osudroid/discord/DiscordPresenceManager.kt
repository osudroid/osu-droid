package com.osudroid.discord

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
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Manages the Discord Rich Presence lifecycle.
 *
 * Uses the Social SDK's unauthenticated Rich Presence RPC path (`Client::SetApplicationId` +
 * `Client::UpdateRichPresence`, without `Client::Connect`), which talks directly to a locally
 * running Discord client.
 *
 * ## Lifecycle
 * 1. [init]: called once from [MainActivity.onLoadEngine]. Starts the callback loop and, if rich
 *    presence is enabled in settings, immediately pushes the current activity.
 * 2. [setActivity] / [clearActivity]: called at scene transitions to update what Discord shows.
 * 3. [disconnect]: called when the app is destroyed to release native resources.
 *
 * ## Threading
 * [setActivity] and [clearActivity] may be called from any thread. The underlying JNI calls are
 * safe because the SDK's `UpdateRichPresence` / `ClearRichPresence` are internally synchronized.
 * The callback loop runs on [Dispatchers.Default].
 *
 * @see [DiscordNative]
 */
object DiscordPresenceManager {
    private const val TAG = "DiscordPresenceManager"

    private val clientId = BuildSettings.DISCORD_CLIENT_ID.toLongOrNull() ?: 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var callbackJob: Job? = null
    private val callbackDelay = 16.milliseconds

    private var isInitialized = false
    private var currentActivity: UserActivity = UserActivity.Idle

    /**
     * Unix epoch milliseconds captured at app launch; used as the presence elapsed-time origin.
     * Stored once so the Discord "elapsed" timer reflects time-in-app, not time-in-scene.
     */
    private var appStartTime = 0L

    /**
     * Initializes the Discord SDK and starts pumping its callback loop. Must be called before
     * [setActivity].
     *
     * If rich presence is already enabled in settings, the current activity is pushed
     * immediately -- there's no connection step to wait for.
     */
    @JvmStatic
    fun init(activity: MainActivity) {
        appStartTime = System.currentTimeMillis()

        if (clientId == 0L) {
            Log.w(TAG, "Discord client ID not configured, skipping initialization.")
            return
        }

        DiscordSocialSdkInit.setEngineActivity(activity)
        DiscordNative.create(clientId)
        isInitialized = true
        startCallbackLoop()
        Log.d(TAG, "SDK initialized with client ID $clientId.")

        if (Config.isDiscordRichPresenceEnabled()) {
            refreshActivity()
        }
    }

    /**
     * Updates the user's Discord rich presence.
     *
     * @param activity The current user activity. Each subclass provides its own status and details.
     */
    @JvmStatic
    fun setActivity(activity: UserActivity) {
        currentActivity = activity

        if (!isInitialized || !Config.isDiscordRichPresenceEnabled()) {
            return
        }

        val online = OnlineManager.getInstance()
        val username = online.username
        val rank = online.rank
        val largeText = if (username.isNotEmpty() && rank > 0) "$username (rank #%,d)".format(rank)
                        else username

        val beatmapUrl = (activity as? UserActivity.InGame)?.beatmapUrl

        Log.d(TAG, "setActivity(${activity::class.simpleName}) details='${activity.details}' state='${activity.status}' party=${activity.partySize}/${activity.partyMax}")

        DiscordNative.updateRichPresence(
            activity.details ?: "", activity.status,
            activity.partySize, activity.partyMax, appStartTime, largeText,
            if (beatmapUrl != null) "View beatmap" else "", beatmapUrl ?: ""
        )
    }

    /**
     * Refreshes the current activity.
     *
     * Call this to update data that would only be present after a certain time (e.g., online rank).
     */
    @JvmStatic
    fun refreshActivity() = setActivity(currentActivity)

    /**
     * Clears the user's Discord rich presence.
     */
    @JvmStatic
    fun clearActivity() {
        if (!isInitialized) {
            return
        }

        Log.d(TAG, "clearActivity().")
        DiscordNative.clearRichPresence()
    }

    /**
     * Clears presence, stops the callback loop, and releases native resources.
     */
    @JvmStatic
    fun disconnect() {
        if (!isInitialized) {
            return
        }

        clearActivity()
        callbackJob?.cancel()
        callbackJob = null
        DiscordNative.destroy()
        isInitialized = false
        Log.d(TAG, "Disconnected from Discord.")
    }

    private fun startCallbackLoop() {
        callbackJob?.cancel()
        callbackJob = scope.launch {
            while (isActive) {
                DiscordNative.runCallbacks()
                delay(callbackDelay)
            }
        }
    }
}
