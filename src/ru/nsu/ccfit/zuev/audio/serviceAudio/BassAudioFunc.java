package ru.nsu.ccfit.zuev.audio.serviceAudio;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_FX;

import java.nio.ByteBuffer;

import ru.nsu.ccfit.zuev.audio.Status;

public class BassAudioFunc {

    public static final int WINDOW_FFT = 1024;

    private int channel = 0;
    private PlayMode mode;
    private long skipPosition;
    private ByteBuffer buffer = null;
    private int playflag = BASS.BASS_STREAM_PRESCAN;
    private boolean isGaming = false;
    private BroadcastReceiver receiver;
    private LocalBroadcastManager broadcastManager;

    BassAudioFunc() {
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
    }

    public boolean pause() {
        return BASS.BASS_ChannelPause(channel);
    }

    private boolean resume() {
        return BASS.BASS_ChannelPlay(channel, false);
    }

    public boolean preLoad(String filePath, PlayMode mode) {
        Log.w("BassAudioFunc", "preLoad File: " + filePath);
        BASS.BASS_CHANNELINFO fx = new BASS.BASS_CHANNELINFO();
        doClear();
        this.mode = mode;
        switch (mode) {
            case MODE_NONE: //None
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, playflag);
                // BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
                break;
            case MODE_HT: //HT
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE | BASS.BASS_STREAM_PRESCAN);
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);
                // BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, -25.0f);
                break;
            case MODE_DT: //DT
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE | BASS.BASS_STREAM_PRESCAN);
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);
                // BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 50.0f);
                break;
            case MODE_NC: //NC
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE | BASS.BASS_STREAM_PRESCAN);
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);
                // BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);

                BASS.BASS_ChannelGetInfo(channel, fx);
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (int) (fx.freq * 1.5));
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0.0f);
                break;
            case MODE_SU: //SU
                channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE | BASS.BASS_STREAM_PRESCAN);
                channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);
                // BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 25.0f);
                break;
        }
        return channel != 0;
    }

    public boolean preLoad(String filePath, float speed, boolean enableNC) {
        if (speed == 1.0f) {
            return preLoad(filePath, PlayMode.MODE_NONE);
        }
        Log.w("BassAudioFunc", "preLoad File: " + filePath);
        BASS.BASS_CHANNELINFO fx = new BASS.BASS_CHANNELINFO();
        doClear();
        this.mode = PlayMode.MODE_SC;
        channel = BASS.BASS_StreamCreateFile(filePath, 0, 0, BASS.BASS_STREAM_DECODE | BASS.BASS_STREAM_PRESCAN);
        channel = BASS_FX.BASS_FX_TempoCreate(channel, BASS.BASS_STREAM_AUTOFREE);
        if (enableNC) {
            BASS.BASS_ChannelGetInfo(channel, fx);
            if (speed > 1.5){
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (int) (fx.freq * 1.5f));
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 1.5f - 1.0f) * 100);
            }
            else if (speed < 0.75){
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (int) (fx.freq * 0.75f));
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed / 0.75f - 1.0f) * 100);
            }
            else {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, (int) (fx.freq * speed));
                BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 0.0f);
            }
        }
        else{
            BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, (speed - 1.0f) * 100);
        }
        return channel != 0;
    }

    public boolean play() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PAUSED) {
            return resume();
        } else if (channel != 0) {
            /*if(!isGaming){
                BASS.BASS_ChannelSetSync(channel, BASS.BASS_SYNC_END, 0, new BASS.SYNCPROC() {
                    @Override
                    public void SYNCPROC(int handle, int channel, int data, Object user) {
                        broadcastManager.sendBroadcast(new Intent("Notify_next"));
                    }
                },0);
            }*/
            BASS.BASS_ChannelSetSync(channel, BASS.BASS_SYNC_END, 0, new BASS.SYNCPROC() {
                @Override
                public void SYNCPROC(int handle, int channel, int data, Object user) {
                    if (!isGaming) {
                        broadcastManager.sendBroadcast(new Intent("Notify_next"));
                    } else {
                        stop();
                    }
                }
            }, 0);
            return BASS.BASS_ChannelPlay(channel, true);
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
            if (skipPosition == 0 || skipPosition == -1)
                skipPosition = BASS.BASS_ChannelSeconds2Bytes(channel, ms / 1000.0);
            if (mode == PlayMode.MODE_NONE)
                return BASS.BASS_ChannelSetPosition(channel, skipPosition, BASS.BASS_POS_BYTE);
            else return BASS.BASS_ChannelSetPosition(channel, skipPosition, BASS.BASS_POS_DECODE);
        }
        return false;
    }

    public Status getStatus() {
        if (channel == 0) return Status.STOPPED;

        switch (BASS.BASS_ChannelIsActive(channel)) {
            case BASS.BASS_ACTIVE_STOPPED:
                return Status.STOPPED;
            case BASS.BASS_ACTIVE_PAUSED:
                return Status.PAUSED;
            case BASS.BASS_ACTIVE_PLAYING:
                return Status.PLAYING;
        }

        return Status.STALLED;
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
        skipPosition = 0;
    }

    public void setLoop(boolean isLoop) {
        if (isLoop) {
            this.playflag = BASS.BASS_SAMPLE_LOOP | BASS.BASS_STREAM_PRESCAN;
        } else {
            this.playflag = BASS.BASS_STREAM_PRESCAN;
        }
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

}
