// File: src/com/rian/andengine/timing/SongServiceClock.kt

package com.rian.andengine.timing

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.audio.Status

/**
 * An [IAdjustableClock] that uses [SongService] (BASS audio wrapper) as its backing source.
 *
 * This adapter converts [SongService]'s playback position into [IAdjustableClock], allowing
 * integration with [FramedBeatmapClock].
 */
class SongServiceClock(private val songService: SongService) : IAdjustableClock {
    override val currentTime
        get() = (songService.getPositionPrecise() / 1000.0).toFloat()

    override var rate = 1f

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