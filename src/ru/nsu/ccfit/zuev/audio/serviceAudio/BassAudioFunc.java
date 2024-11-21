package ru.nsu.ccfit.zuev.audio.serviceAudio;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_FX;

import java.nio.ByteBuffer;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Config;

public class BassAudioFunc {

    public static final int WINDOW_FFT = 1024;

    private int channel = 0;
    private float speed = 1f;
    private boolean adjustPitch;
    private final BASS.BASS_CHANNELINFO channelInfo = new BASS.BASS_CHANNELINFO();

    private ByteBuffer buffer = null;
    private int playFlag = BASS.BASS_STREAM_PRESCAN;
    private boolean isGaming = false;
    private BroadcastReceiver receiver;
    private LocalBroadcastManager broadcastManager;

    /**
     * The channel's frequency, in Hz.
     */
    private float frequency;

    /**
     * Whether the game is currently on focus.
     */
    private boolean onFocus;

    /**
     * The playback buffer length that is used when the game is on focus, in seconds.
     * <br>
     * This is pretty low to achieve the smallest latency possible without introducing CPU overhead.
     */
    private final float onFocusBufferLength = 0.1f;

    /**
     * The playback buffer length that is used when the game is not on focus, in seconds.
     * <br>
     * This is a lot higher than the value used in {@link #onFocusBufferLength} to reduce CPU usage.
     */
    private final float offFocusBufferLength = 0.5f;

    public BassAudioFunc() {
    }

    public void onGameResume() {
        onFocus = true;

        // Use a lower update period to minimize latency. This results in higher CPU usage, but it is necessary
        // to provide a smooth gameplay experience.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5);

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, onFocusBufferLength);
        }
    }

    public void onGamePause() {
        onFocus = false;

        // Use a higher update period to reduce CPU usage. Minimum latency is not required here,
        // only smooth audio playback without stutters.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 100);

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, offFocusBufferLength);
        }
    }

    public boolean pause() {
        return BASS.BASS_ChannelPause(channel);
    }

    public boolean resume() {
        setEndSync();

        if (BASS.BASS_ChannelPlay(channel, false))
        {
            setVolume(Config.getBgmVolume());
            return true;
        }
        return false;
    }

    public boolean preLoad(String filePath, float speed, boolean adjustPitch) {
        doClear();

        channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, playFlag | BASS.BASS_STREAM_DECODE);
        channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);

        if (channel == 0) {
            this.speed = 1;
            this.adjustPitch = false;

            return false;
        }

        BASS.BASS_ChannelGetInfo(channel, channelInfo);
        frequency = channelInfo.freq;

        setSpeed(speed);
        setAdjustPitch(adjustPitch);

        // Use smaller buffer length on focus for smaller latency.
        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_BUFFER, onFocus ? onFocusBufferLength : offFocusBufferLength);

        return true;
    }

    public boolean play() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PAUSED) {
            return resume();
        } else if (channel != 0) {
            setEndSync();
            if (BASS.BASS_ChannelPlay(channel, true))
            {
                setVolume(Config.getBgmVolume());
                return true;
            }
        }
        return false;
    }

    public boolean stop() {
        if (channel != 0) {
            BASS.BASS_ChannelStop(channel);
            return BASS.BASS_StreamFree(channel);
        }
        return false;
    }

    public boolean jump(int ms) {
        if (channel != 0 && ms > 0) {
            long skipPosition = BASS.BASS_ChannelSeconds2Bytes(channel, ms / 1000.0);

            return BASS.BASS_ChannelSetPosition(channel, skipPosition,
                    speed == 1f ? BASS.BASS_POS_BYTE : BASS.BASS_POS_DECODE);
        }

        return false;
    }

    public Status getStatus() {
        if (channel == 0) return Status.STOPPED;

        return switch (BASS.BASS_ChannelIsActive(channel)) {
            case BASS.BASS_ACTIVE_STOPPED -> Status.STOPPED;
            case BASS.BASS_ACTIVE_PAUSED -> Status.PAUSED;
            case BASS.BASS_ACTIVE_PLAYING -> Status.PLAYING;
            default -> Status.STALLED;
        };
    }

    public int getPosition() {
        if (channel != 0) {
            long pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE);
            if (pos != -1) {
                return (int) (BASS.BASS_ChannelBytes2Seconds(channel, pos) * 1000);
            }
        }
        return 0;
    }

    public int getLength() {
        if (channel != 0) {
            long length = BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE);
            if (length != -1) {
                return (int) (BASS.BASS_ChannelBytes2Seconds(channel, length) * 1000);
            }
        }
        return 0;
    }

    public float[] getSpectrum() {
        if (BASS.BASS_ChannelIsActive(channel) != BASS.BASS_ACTIVE_PLAYING) {
            return null;
        }
        if (buffer == null) {
            buffer = ByteBuffer.allocateDirect(WINDOW_FFT << 1);
            buffer.order(null);
        }
        BASS.BASS_ChannelGetData(channel, buffer, BASS.BASS_DATA_FFT1024);
        int resSize = WINDOW_FFT >> 1;
        float[] spectrum = new float[resSize];
        buffer.asFloatBuffer().get(spectrum);
        return spectrum;
    }

    private void doClear() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelStop(channel);
        }
        BASS.BASS_StreamFree(channel);
    }

    public void setLoop(boolean isLoop) {
        if (isLoop) {
            playFlag |= BASS.BASS_SAMPLE_LOOP;
        } else {
            playFlag ^= BASS.BASS_SAMPLE_LOOP;
        }
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        onAudioEffectChange();
    }

    public void setAdjustPitch(boolean adjustPitch) {
        this.adjustPitch = adjustPitch;
        onAudioEffectChange();
    }

    public void setFrequencyForcefully(float frequency) {
        if (channel == 0) {
            return;
        }
        this.frequency = frequency;
        BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_FREQ, frequency);
        BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0);
    }

    public float getFrequency() {
        return frequency;
    }

    public float getVolume() {
        BASS.FloatValue volume = new BASS.FloatValue();
        if (channel != 0) {
            BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume);
        }
        return volume.value;
    }

    public void setVolume(float volume) {
        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume);
        }
    }

    public void setGaming(boolean isGaming) {
        System.out.println("Audio Service Running In Game: " + isGaming);
        this.isGaming = isGaming;
    }

    public void setReciverStuff(BroadcastReceiver receiver, IntentFilter filter, Context context) {
        this.receiver = receiver;
        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
            broadcastManager.registerReceiver(receiver, filter);
        }
    }

    public void unregisterReceiverBM() {
        if (broadcastManager != null) broadcastManager.unregisterReceiver(receiver);
    }

    public void freeALL() {
        BASS.BASS_Free();
    }

    private void setEndSync() {
        BASS.BASS_ChannelSetSync(channel, BASS.BASS_SYNC_END, 0, (handle, channel, data, user) -> {
            if (!isGaming) {
                broadcastManager.sendBroadcast(new Intent("Notify_next"));
            } else {
                stop();
            }
        }, 0);
    }

    private void onAudioEffectChange() {
        if (channel == 0) {
            return;
        }


        if (adjustPitch) {
            frequency = channelInfo.freq * speed;
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_FREQ, frequency);
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0);
        } else {
            frequency = channelInfo.freq;
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO_FREQ, frequency);
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed - 1) * 100);
        }
    }
}
