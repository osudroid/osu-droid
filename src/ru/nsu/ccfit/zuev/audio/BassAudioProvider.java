package ru.nsu.ccfit.zuev.audio;

import android.content.res.AssetManager;

import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_FX;

import java.nio.ByteBuffer;

/**
 * @author chenkang.ck
 */
public class BassAudioProvider {

    public static final int DECODER_NORMAL = 0;
    public static final int DECODER_DOUBLE_TIME = 1;
    public static final int DECODER_NIGHT_CORE = 2;
    public static final int WINDOW_FFT = 1024;

    private int channel = 0;
    private BASS.FloatValue freq = new BASS.FloatValue();
    private int fileFlag = 0;
    private int decoder = 0;
    private int multiplier = 0;

    private ByteBuffer buffer = null;

    public BassAudioProvider() {
        freq.value = 1.0f;
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
        // BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 100);
        // BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 10);
    }

    public boolean prepare(final String fileName) {
        free();
        if (fileName != null && fileName.length() > 0) {
            channel = BASS.BASS_StreamCreateFile(fileName, 0, 0, fileFlag);  // BASS.BASS_STREAM_DECODE
            if (decoder > 0) {
                channel = BASS_FX.BASS_FX_TempoCreate(channel, 0);
                BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq);

                if (decoder == DECODER_DOUBLE_TIME) {
                    float targetTempo = multiplier - 100.0f;
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value);
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, targetTempo);

                } else if (decoder == DECODER_NIGHT_CORE) {
                    float targetFreq = multiplier / 100.0f;
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value * targetFreq);
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 1.0f);
                }
            }
        }
        return channel != 0;
    }

    public boolean prepare(final AssetManager manager, final String assetName) {
        free();
        if (manager != null && assetName != null && assetName.length() > 0) {
            BASS.Asset asset = new BASS.Asset(manager, assetName);
            channel = BASS.BASS_StreamCreateFile(asset, 0, 0, fileFlag);
            if (decoder > 0) {
                channel = BASS_FX.BASS_FX_TempoCreate(channel, 0);
                BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq);

                if (decoder == DECODER_DOUBLE_TIME) {
                    float targetTempo = multiplier - 100.0f;
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value);
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, targetTempo);

                } else if (decoder == DECODER_NIGHT_CORE) {
                    float targetFreq = multiplier / 100.0f;
                    BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, freq.value * targetFreq);
                    BASS.BASS_ChannelSetAttribute(channel, BASS_FX.BASS_ATTRIB_TEMPO, 1.0f);
                }
            }
        }
        return channel != 0;
    }

    public void play() {
        if (channel != 0) {
            if (BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PAUSED) {
                BASS.BASS_ChannelPlay(channel, false);
            } else {
                BASS.BASS_ChannelPlay(channel, true);
            }
        }
    }

    public void pause() {
        if (channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelPause(channel);
        }
    }

    public void stop() {
        if (channel != 0) {
            BASS.BASS_ChannelStop(channel);
        }
    }

    public void seek(double sec) {
        if (channel != 0) {
            long playPos = BASS.BASS_ChannelSeconds2Bytes(channel, sec);
            BASS.BASS_ChannelSetPosition(channel, playPos, BASS.BASS_POS_DECODETO);
        }
    }

    public void free() {
        if (isPlaying()) {
            stop();
        }
        BASS.BASS_StreamFree(channel);
        channel = 0;
    }

    public float[] getSpectrum() {
        if (!isPlaying()) {
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

    public int getErrorCode() {
        return BASS.BASS_ErrorGetCode();
    }

    public Status getStatus() {
        if (channel == 0)
            return Status.STOPPED;

        final int playerStatus = BASS.BASS_ChannelIsActive(channel);

        if (playerStatus == BASS.BASS_ACTIVE_STOPPED)
            return Status.STOPPED;
        else if (playerStatus == BASS.BASS_ACTIVE_PLAYING)
            return Status.PLAYING;
        else if (playerStatus == BASS.BASS_ACTIVE_PAUSED)
            return Status.PAUSED;
        return Status.STALLED;
    }

    public boolean isPlaying() {
        return channel != 0 && BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING;
    }

    public double getPosition() {
        if (channel != 0) {
            long pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE);
            if (pos != -1) {
                return BASS.BASS_ChannelBytes2Seconds(channel, pos);
            }
        }
        return 0f;
    }

    public double getLength() {
        if (channel != 0) {
            long length = BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE);
            if (length != -1) {
                return BASS.BASS_ChannelBytes2Seconds(channel, length);
            }
        }
        return 0f;
    }

    public void setLoop() {
        fileFlag |= BASS.BASS_SAMPLE_LOOP;
    }

    public void setUseSoftDecoder(int decoder) {
        if (decoder > 0) {
            this.fileFlag |= BASS.BASS_STREAM_DECODE;
        } else {
            this.fileFlag = 0;
        }
        this.decoder = decoder;
    }

    public void setDecoderMultiplier(int multiplier) {
        this.multiplier = multiplier;
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
}
