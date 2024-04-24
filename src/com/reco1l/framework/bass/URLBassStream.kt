package com.reco1l.framework.bass

import com.un4seen.bass.BASS
import ru.nsu.ccfit.zuev.audio.Status

class URLBassStream(url: String, onEnd: (URLBassStream) -> Unit = {}) {


    private var id = 0


    val status: Status
        get() {
            if (id == 0) {
                return Status.STOPPED
            }

            val native = BASS.BASS_ChannelIsActive(id)

            return when (native) {
                BASS.BASS_ACTIVE_STOPPED -> Status.STOPPED
                BASS.BASS_ACTIVE_PLAYING -> Status.PLAYING
                BASS.BASS_ACTIVE_PAUSED -> Status.PAUSED
                else -> Status.STALLED
            }
        }


    init {

        id = BASS.BASS_StreamCreateURL(url, 0, BASS.BASS_STREAM_AUTOFREE, null, null)

        if (id == 0) {
            throw IllegalStateException("Failed to start stream, error code: ${BASS.BASS_ErrorGetCode()}")
        } else {
            BASS.BASS_ChannelSetSync(id, BASS.BASS_SYNC_END, 0, { _, _, _, _ -> onEnd(this) }, 0)
        }

    }

    fun play() {
        if (id == 0) {
            return
        }

        if (BASS.BASS_ChannelIsActive(id) == BASS.BASS_ACTIVE_PAUSED) {
            BASS.BASS_ChannelPlay(id, false)
        } else {
            BASS.BASS_ChannelPlay(id, true)
        }
    }

    fun pause() {
        if (id != 0 && BASS.BASS_ChannelIsActive(id) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelPause(id)
        }
    }

    fun stop() {
        if (id != 0) {
            BASS.BASS_ChannelStop(id)
        }
    }

    fun free() {
        if (status == Status.PLAYING) {
            stop()
        }
        BASS.BASS_StreamFree(id)
        id = 0
    }

    fun setVolume(volume: Float) {
        if (id != 0) {
            BASS.BASS_ChannelSetAttribute(id, BASS.BASS_ATTRIB_VOL, volume)
        }
    }
}
