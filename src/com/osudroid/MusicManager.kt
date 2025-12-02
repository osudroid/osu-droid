package com.osudroid

import com.osudroid.data.BeatmapInfo
import com.rian.osu.beatmap.parser.BeatmapParser
import com.rian.osu.beatmap.timings.ControlPoint
import com.rian.osu.beatmap.timings.EffectControlPoint
import com.rian.osu.beatmap.timings.EffectControlPointManager
import com.rian.osu.beatmap.timings.TimingControlPoint
import com.rian.osu.beatmap.timings.TimingControlPointManager
import org.anddev.andengine.engine.handler.IUpdateHandler
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.GlobalManager

object MusicManager : IUpdateHandler {

    /**
     * The currently loaded beatmap.
     */
    var currentBeatmap: BeatmapInfo? = null
        set(value) {
            if (field != value) {
                field = value
                onBeatmapChangeListeners.values.forEach { it(value) }
            }
        }

    /**
     * Whether the music manager is currently loading a beatmap or not.
     */
    var isLoading = false
        private set

    /**
     * The current position of the song in milliseconds.
     */
    var position
        get() = songService.position
        set(value) = songService.seekTo(value)


    /**
     * Whether a song is currently playing or not.
     */
    val isPlaying
        get() = songService.status == Status.PLAYING

    /**
     * The length of the current song in milliseconds.
     */
    val length
        get() = songService.length


    /**
     * The current timing control point.
     */
    var currentTimingControlPoint = TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT

    /**
     * The current effect control point.
     */
    var currentEffectControlPoint = EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT


    private var onBeatmapChangeListeners = mapOf<Any, (BeatmapInfo?) -> Unit>()

    private var timingControlPointManager: TimingControlPointManager? = null
    private var effectControlPointManager: EffectControlPointManager? = null


    private val songService = GlobalManager.getInstance().songService


    private fun clear() {
        timingControlPointManager = null
        effectControlPointManager = null
        currentTimingControlPoint = TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
        currentEffectControlPoint = EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
    }


    fun load() {
        if (isLoading) {
            return
        }
        isLoading = true

        val beatmap = currentBeatmap

        if (beatmap == null) {
            clear()
            isLoading = false
            return
        }

        try {
            BeatmapParser(beatmap.path).use { parser ->

                val data = parser.parse(false)

                if (data == null) {
                    clear()
                    isLoading = false
                    return
                }

                timingControlPointManager = data.controlPoints.timing
                effectControlPointManager = data.controlPoints.effect

                currentTimingControlPoint = timingControlPointManager?.controlPointAt(0.0) ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
                currentEffectControlPoint = effectControlPointManager?.controlPointAt(0.0) ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT

                songService.preLoad(beatmap.audioPath)

                isLoading = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        if (songService.status != Status.PLAYING) {
            songService.play()
        }
    }

    fun stop() {
        if (songService.status == Status.PLAYING) {
            songService.stop()
        }
    }

    fun pause() {
        if (songService.status == Status.PLAYING) {
            songService.pause()
        }
    }

    fun addOnBeatmapChangeListener(key: Any, listener: (BeatmapInfo?) -> Unit) {
        onBeatmapChangeListeners = onBeatmapChangeListeners + (key to listener)
    }

    fun removeOnBeatmapChangeListener(key: Any) {
        onBeatmapChangeListeners = onBeatmapChangeListeners - key
    }


    override fun onUpdate(secondsElapsed: Float) {
        if (songService.status == Status.PLAYING) {
            val positon = songService.position

            currentTimingControlPoint = timingControlPointManager?.controlPointAt(positon.toDouble()) ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.controlPointAt(positon.toDouble()) ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
        } else {
            currentTimingControlPoint = timingControlPointManager?.defaultControlPoint ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.defaultControlPoint ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
        }

        RythimManager.onUpdate(secondsElapsed)
    }

    override fun reset() {}

}

object RythimManager : IUpdateHandler {

    /**
     * The current beat index.
     */
    var beatIndex = 0
        private set(value) {
            if (field != value) {
                field = value
                onBeatChangeListeners.values.forEach { it() }
            }
        }

    /**
     * The current beat signature.
     */
    var beatSignature = 4
        private set(value) {
            if (field != value) {
                field = value
                beatIndex = 0
            }
        }

    /**
     * The elapsed time since the last beat in milliseconds.
     */
    var beatElapsed = 0.0
        private set

    /**
     * The current beat length in milliseconds.
     */
    var beatLength = 0.0
        private set


    /**
     * Whether the current section is in kiai time or not.
     */
    var isKiai = false
        private set


    private var onBeatChangeListeners = mapOf<Any, () -> Unit>()


    private fun updateControlPointInformation(secondsElapsed: Float, timingPoint: TimingControlPoint, effectPoint: EffectControlPoint) {
        beatSignature = timingPoint.timeSignature
        beatLength = timingPoint.msPerBeat
        isKiai = effectPoint.isKiai

        beatElapsed += secondsElapsed * 1000f

        while (beatElapsed >= beatLength) {
            beatElapsed -= beatLength.toFloat()
            beatIndex = (beatIndex + 1) % beatSignature
        }
    }


    override fun onUpdate(secondsElapsed: Float) {
        if (MusicManager.isPlaying) {
            updateControlPointInformation(
                secondsElapsed,
                MusicManager.currentTimingControlPoint,
                MusicManager.currentEffectControlPoint
            )
        } else {
            updateControlPointInformation(
                secondsElapsed,
                TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT,
                EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
            )
        }
    }

    override fun reset() {}


    fun addOnBeatChangeListener(key: Any, listener: () -> Unit) {
        onBeatChangeListeners = onBeatChangeListeners + (key to listener)
    }

    fun removeOnBeatChangeListener(key: Any) {
        onBeatChangeListeners = onBeatChangeListeners - key
    }
}