package com.osudroid.audio

import com.rian.andengine.timing.IAdjustableClock
import com.osudroid.game.FramedBeatmapClock
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService

/**
 * An [IAdjustableClock] that uses [SongService] as its backing source.
 *
 * This adapter converts [SongService]'s playback position into an [IAdjustableClock], allowing integration with
 * [FramedBeatmapClock].
 */
class SongServiceClock(private val songService: SongService) : IAdjustableClock {
    override val currentTime
        get() = (songService.getPositionPrecise() / 1000.0).toFloat()

    override var rate = 1f
        set(value) {
            field = value
            songService.setSpeed(value)
        }

    override val isRunning
        get() = songService.getStatus() == Status.PLAYING

    override fun reset() {
        songService.seekTo(0)
        songService.stop()
    }

    override fun start() {
        songService.play()
    }

    override fun stop() {
        songService.pause()
    }

    override fun seek(position: Float): Boolean {
        // Clamp position to valid range [0, trackLength].
        val trackLength = songService.getLength() / 1000f

        // Avoid allocation
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (position < 0f || position > trackLength) {
            return false
        }

        val positionMs = (position * 1000).toInt()
        songService.seekTo(positionMs)
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}