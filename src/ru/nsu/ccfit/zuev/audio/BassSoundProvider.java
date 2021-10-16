package ru.nsu.ccfit.zuev.audio;

import ru.nsu.ccfit.zuev.osu.Config;

import android.content.res.AssetManager;

import com.un4seen.bass.BASS;

/**
 * @author chenkang.ck
 */
public class BassSoundProvider {

    private static final int SIMULTANEOUS_PLAYBACKS = 8;

    private int sample = 0;
    private int channel = 0;

    public BassSoundProvider() {
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_LATENCY);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 0);
    }

    public boolean prepare(final String fileName) {
        free();
        if (fileName != null && fileName.length() > 0) {
            sample = BASS.BASS_SampleLoad(fileName, 0, 0, SIMULTANEOUS_PLAYBACKS, BASS.BASS_SAMPLE_OVER_POS);
        }
        return sample != 0;
    }

    public boolean prepare(final AssetManager manager, final String assetName) {
        free();
        if (manager != null && assetName != null && assetName.length() > 0) {
            BASS.Asset asset = new BASS.Asset(manager, assetName);
            sample = BASS.BASS_SampleLoad(asset, 0, 0, SIMULTANEOUS_PLAYBACKS, BASS.BASS_SAMPLE_OVER_POS);
        }
        return sample != 0;
    }

    public void play() {
        play(Config.getSoundVolume());
    }

    public void play(float volume) {
        if (sample != 0) {
            channel = BASS.BASS_SampleGetChannel(sample, false);
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
            BASS.BASS_ChannelPlay(channel, false);
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume * Config.getSoundVolume());
        }
    }

    public void stop() {
        if (sample != 0) {
            BASS.BASS_ChannelStop(channel);
        }
    }

    public void free() {
        stop();
        BASS.BASS_SampleFree(sample);
        sample = 0;
    }

    public void setLooping(boolean looping) {
        // not impl
    }
}
