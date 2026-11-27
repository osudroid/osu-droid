package com.osudroid.beatmaplisting

import android.util.Log
import androidx.annotation.MainThread
import com.osudroid.utils.mainThread
import com.reco1l.framework.bass.URLBassStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.ToastLogger

/**
 * Plays beatmap set previews in the beatmap listing, one at a time.
 */
@MainThread
class BeatmapPreviewPlayer {
    private val _beatmapSetId = MutableStateFlow<Long?>(null)

    /**
     * The ID of the beatmap set whose preview is loading or playing, or `null` if there is none.
     */
    val beatmapSetId = _beatmapSetId.asStateFlow()

    // State is only accessed on the main thread.
    // The stream's connection runs elsewhere, so starting and stopping a preview cannot interleave.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var loadJob: Job? = null

    /**
     * Identifies the stream being connected, so that its connection can be aborted.
     */
    private var loadRequest: Any? = null

    private var stream: URLBassStream? = null

    /**
     * Whether the main menu's music was paused to play a preview and has not been resumed yet.
     */
    private var isMusicPausedByPreview = false

    private var recentPlayCount = 0
    private var lastPlayTime = 0L

    /**
     * Stops the preview of [beatmapSet] if it is loading or playing, or plays it otherwise.
     */
    fun toggle(beatmapSet: BeatmapSetModel) {
        if (beatmapSetId.value == beatmapSet.id) {
            stop()
        } else {
            play(beatmapSet)
        }
    }

    /**
     * Stops the current preview, if any.
     *
     * If the main menu's music was paused to play the preview, it is resumed.
     */
    fun stop() = stop(true)

    private fun play(beatmapSet: BeatmapSetModel) {
        if (recentPlayCount >= MAX_RECENT_PLAYS) {
            if (System.currentTimeMillis() - lastPlayTime <= PLAY_COOLDOWN_MS) {
                ToastLogger.showText("Please wait a few seconds before trying again.", false)
                return
            }

            recentPlayCount = 0
        }

        // The next preview pauses the music again, so there is no need to resume it in between.
        stop(false)

        val url = BeatmapListing.mirror.preview.request(beatmapSet.beatmaps[0].id).toString()
        val request = Any()

        _beatmapSetId.value = beatmapSet.id
        loadRequest = request
        loadJob = scope.launch { load(url, request) }
    }

    private suspend fun CoroutineScope.load(url: String, request: Any) {
        var created: URLBassStream? = null

        try {
            // The stream is stored outside the block. withContext discards the block's result
            // if this job is canceled while the block is finishing.
            withContext(Dispatchers.IO) {
                created = URLBassStream(url, request) { ended ->
                    mainThread {
                        // A newer preview may have started by the time this runs.
                        if (stream === ended) {
                            stop()
                        }
                    }
                }
            }

            val stream = created!!
            this@BeatmapPreviewPlayer.stream = stream
            loadRequest = null

            if (GlobalManager.getInstance().songService.status == Status.PLAYING) {
                GlobalManager.getInstance().mainScene.musicControl(MusicOption.PAUSE)
                isMusicPausedByPreview = true
            }

            stream.setVolume(Config.getBgmVolume())
            stream.play()

            recentPlayCount++
            lastPlayTime = System.currentTimeMillis()
        } catch (e: Exception) {
            if (stream !== created) {
                created?.free()
            }

            // An aborted connection also fails to create the stream, which is not an error.
            ensureActive()

            if (e is CancellationException) {
                throw e
            }

            Log.e("BeatmapPreviewPlayer", "Failed to load preview", e)
            stop()
        }
    }

    private fun stop(resumeMusic: Boolean) {
        loadJob?.cancel()
        loadJob = null

        try {
            loadRequest?.let(URLBassStream::cancel)
            stream?.free()
        } catch (e: Exception) {
            Log.e("BeatmapPreviewPlayer", "Failed to stop preview", e)
        }

        loadRequest = null
        stream = null

        if (resumeMusic && isMusicPausedByPreview) {
            GlobalManager.getInstance().mainScene.musicControl(MusicOption.PLAY)
            isMusicPausedByPreview = false
        }

        _beatmapSetId.value = null
    }

    companion object {
        /**
         * The number of previews that can be played before [PLAY_COOLDOWN_MS] has to pass.
         */
        private const val MAX_RECENT_PLAYS = 2

        /**
         * The time in milliseconds after the last preview started before more previews can be played,
         * once [MAX_RECENT_PLAYS] previews have been played.
         */
        private const val PLAY_COOLDOWN_MS = 5000L
    }
}
