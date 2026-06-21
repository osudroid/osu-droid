package com.osudroid.discord

import android.content.Context
import android.content.Intent
import android.util.Log
import com.discord.socialsdk.DiscordSocialSdkInit
import com.osudroid.BuildSettings
import com.osudroid.utils.mainThread
import java.net.URLEncoder
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.SecurityUtils
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
 * ## Token exchange
 * All token operations (auth-code exchange and refresh) are performed server-side via the game server API. The JNI
 * layer surfaces only the auth code and PKCE verifier. Refresh tokens are owned entirely by Kotlin and the server.
 *
 * ## Threading
 * [setActivity] and [clearActivity] may be called from any thread. The underlying JNI calls are
 * safe because the SDK's `UpdateRichPresence` / `ClearRichPresence` are internally synchronized.
 * The callback loop runs on [Dispatchers.Default], whereas HTTP calls run on [Dispatchers.IO].
 *
 * @see [DiscordNative]
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
    var isPendingAuthorization = false
        private set

    /**
     * `true` while a connection attempt is in progress but not yet fully established.
     * Covers both the OAuth flow ([isPendingAuthorization]) and the silent refresh-token reconnect.
     */
    val isConnecting
        get() = !isConnected && callbackJob?.isActive == true

    private var connectionStateListener: (() -> Unit)? = null
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

        if (isConnected) {
            Log.d(TAG, "connect() ignored: already connected.")
            return
        }

        if (isPendingAuthorization) {
            Log.d(TAG, "connect() ignored: authorization already in progress.")
            return
        }

        if (callbackJob?.isActive == true) {
            Log.d(TAG, "connect() ignored: connection already in progress.")
            return
        }

        val savedRefreshToken = loadRefreshToken()

        if (savedRefreshToken != null) {
            Log.d(TAG, "Reconnecting with stored refresh token.")
            startCallbackLoop()

            scope.launch { exchangeRefreshToken(savedRefreshToken) }
        } else {
            Log.d(TAG, "No saved refresh token, starting authorization flow.")
            DiscordNative.authorize(clientId)
            isPendingAuthorization = true
            startCallbackLoop()
        }
    }

    /**
     * Registers a listener that is invoked on the main thread whenever [isConnected] changes.
     *
     * Pass `null` to unregister.
     */
    @JvmStatic
    fun setConnectionStateListener(listener: (() -> Unit)?) {
        connectionStateListener = listener
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
        var wasReady = false

        callbackJob = scope.launch {
            while (isActive) {
                DiscordNative.runCallbacks()

                if (DiscordNative.hasAuthorizationCode()) {
                    val code = DiscordNative.getAuthorizationCode()
                    val verifier = DiscordNative.getVerifier()
                    val redirectUri = DiscordNative.getRedirectUri()
                    DiscordNative.clearAuthorizationCode()

                    launch { exchangeAuthorizationCode(code, verifier, redirectUri) }
                }

                if (DiscordNative.hasAuthorizationFailed()) {
                    DiscordNative.clearAuthorizationFailed()
                    isPendingAuthorization = false
                    Log.w(TAG, "Authorization cancelled or rejected by user, stopping.")
                    stopCallbackLoop()
                    return@launch
                }

                val isNowReady = DiscordNative.isReady()

                if (isNowReady && !wasReady) {
                    isConnected = true
                    Log.d(TAG, "Discord ready.")
                    mainThread { connectionStateListener?.invoke() }
                    refreshActivity()
                } else if (!isNowReady && wasReady) {
                    isConnected = false
                    Log.d(TAG, "Discord disconnected.")
                    mainThread { connectionStateListener?.invoke() }
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
     *
     * We skip the abort if the auth code has already been received — that means the user did
     * authorize and the server exchange is in progress (or just completed). Aborting at that
     * point would wrongly cancel a successful consent.
     */
    fun onActivityResume() {
        if (isPendingAuthorization && !DiscordNative.hasAuthorizationCode()) {
            Log.d(TAG, "onActivityResume: aborting pending authorization.")
            DiscordNative.abortAuthorize()
        }
    }

    /**
     * Exchanges an authorization code (from the OAuth2 PKCE flow) for access and refresh
     * tokens via the game server, then feeds the access token into the Discord SDK.
     */
    private suspend fun exchangeAuthorizationCode(code: String, verifier: String, redirectUri: String) {
        // This is not blocking, but IDE will warn due to UnsupportedEncodingException throw.
        @Suppress("BlockingMethodInNonBlockingContext")
        val encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8")

        val sign = SecurityUtils.signRequest("${code}_${verifier}_${encodedRedirectUri}") ?: run {
            Log.w(TAG, "exchangeAuthorizationCode: could not sign request (app signature unavailable).")
            isPendingAuthorization = false
            return
        }

        try {
            val responseJson = withContext(Dispatchers.IO) {
                val body = FormBody.Builder()
                    .add("code", code)
                    .add("code_verifier", verifier)
                    .add("redirect_uri", redirectUri)
                    .add("sign", sign)
                    .build()

                val request = Request.Builder()
                    .url(OnlineManager.endpoint + "discord_exchange.php")
                    .post(body)
                    .build()

                OnlineManager.client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    JSONObject(response.body!!.string())
                }
            }

            val accessToken = responseJson.getString("access_token")
            val refreshToken = responseJson.getString("refresh_token")

            saveRefreshToken(refreshToken)
            DiscordNative.provideTokens(accessToken)
            bringGameToFront()

            Log.d(TAG, "Authorization code exchange successful.")
        } catch (e: Exception) {
            Log.w(TAG, "Authorization code exchange failed: ${e.message}")
            // Stop the loop so the SDK doesn't remain in a state where it authorized but never
            // received UpdateToken, which can cause it to re-open Discord on runCallbacks() ticks.
            stopCallbackLoop()
        } finally {
            isPendingAuthorization = false
        }
    }

    /**
     * Exchanges a saved refresh token for a new access and refresh token pair via the game server.
     *
     * On a 401 (token revoked or expired), clears the saved token and stops. The user must reconnect manually via
     * the "Connect to Discord" button, which will start a fresh OAuth flow.
     *
     * On network or server errors, this fails silently. The user can retry via the "Connect to Discord" button.
     */
    private suspend fun exchangeRefreshToken(savedToken: String) {
        val sign = SecurityUtils.signRequest(savedToken) ?: run {
            Log.w(TAG, "exchangeRefreshToken: could not sign request (app signature unavailable).")
            return
        }

        try {
            val (isRevoked, responseJson) = withContext(Dispatchers.IO) {
                val body = FormBody.Builder()
                    .add("refresh_token", savedToken)
                    .add("sign", sign)
                    .build()

                val request = Request.Builder()
                    .url(OnlineManager.endpoint + "discord_refresh.php")
                    .post(body)
                    .build()

                OnlineManager.client.newCall(request).execute().use { response ->
                    Pair(response.code == 401, if (response.isSuccessful) JSONObject(response.body!!.string()) else null)
                }
            }

            if (isRevoked) {
                Log.w(TAG, "Refresh token rejected (401), clearing token. User must reconnect manually.")
                clearSavedRefreshToken()
                stopCallbackLoop()
                return
            }

            checkNotNull(responseJson) { "No response body" }

            val accessToken = responseJson.getString("access_token")
            val newRefreshToken = responseJson.getString("refresh_token")

            saveRefreshToken(newRefreshToken)
            DiscordNative.provideTokens(accessToken)

            Log.d(TAG, "Refresh token exchange successful.")
        } catch (e: Exception) {
            Log.w(TAG, "Refresh token exchange failed: ${e.message}")
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
