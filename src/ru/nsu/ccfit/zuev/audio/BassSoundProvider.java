package ru.nsu.ccfit.zuev.audio;

import ru.nsu.ccfit.zuev.osu.Config;

import android.content.res.AssetManager;

import com.un4seen.bass.BASS;

/**
 * @author chenkang.ck
 */
public class BassSoundProvider {

    public static final BassSoundProvider EMPTY = new BassSoundProvider();

    private int sample = 0;
    private int channel = 0;
    private boolean looping;

    /**
     * The rate at which the sound is played back (affects pitch). 1 is 100% playback speed, or default frequency.
     */
    private float frequency = 1f;

    private final BASS.BASS_SAMPLE sampleInfo = new BASS.BASS_SAMPLE();

    public boolean prepare(final String fileName) {
        free();

        if (fileName != null && !fileName.isEmpty()) {
            sample = BASS.BASS_SampleLoad(fileName, 0, 0, 1, BASS.BASS_SAMPLE_OVER_POS);
            BASS.BASS_SampleGetInfo(sample, sampleInfo);
            applyAudioEffectsToSample();
        } else {
            sample = 0;
        }

        return sample != 0;
    }

    public boolean prepare(final AssetManager manager, final String assetName) {
        free();

        if (manager != null && assetName != null && !assetName.isEmpty()) {
            BASS.Asset asset = new BASS.Asset(manager, assetName);
            sample = BASS.BASS_SampleLoad(asset, 0, 0, 1, BASS.BASS_SAMPLE_OVER_POS);
            BASS.BASS_SampleGetInfo(sample, sampleInfo);
            applyAudioEffectsToSample();
        } else {
            sample = 0;
        }

        return sample != 0;
    }

    public void play() {
        play(1);
    }

    public void play(float volume) {
        if (sample == 0) {
            return;
        }

        if (volume == 0) {
            stop();
            return;
        }

        if (channel == 0) {
            channel = BASS.BASS_SampleGetChannel(sample, BASS.BASS_SAMCHAN_STREAM);

            if (channel == 0) {
                return;
            }

            applyAudioEffectsToChannel();
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_NOBUFFER, 1);
        }

        if (channel == 0) {
            return;
        }

        // Ensure the current playback is stopped first.
        stop();

        BASS.BASS_ChannelPlay(channel, true);
        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, volume * Config.getSoundVolume());
    }

    public void stop() {
        if (channel == 0) {
            return;
        }

        if (BASS.BASS_ChannelIsActive(channel) == BASS.BASS_ACTIVE_PLAYING) {
            BASS.BASS_ChannelStop(channel);
        }
    }

    public void free() {
        if (sample == 0) {
            return;
        }

        BASS.BASS_SampleFree(sample);
        sample = 0;
        channel = 0;
    }

    public void setLooping(boolean looping) {
        // Prevent redundant processing when state is already correct.
        if (this.looping == looping) {
            return;
        }

        this.looping = looping;

        applyAudioEffectsToSample();
        applyAudioEffectsToChannel();
    }

    public void setFrequency(float frequency) {
        // Prevent redundant processing when state is already correct.
        if (this.frequency == frequency) {
            return;
        }

        this.frequency = frequency;

        applyAudioEffectsToChannel();
    }

    private void applyAudioEffectsToSample() {
        if (sample == 0) {
            return;
        }

        if (looping) {
            sampleInfo.flags |= BASS.BASS_SAMPLE_LOOP;
        } else {
            sampleInfo.flags ^= BASS.BASS_SAMPLE_LOOP;
        }

        BASS.BASS_SampleSetInfo(sample, sampleInfo);
    }

    private void applyAudioEffectsToChannel() {
        if (channel == 0) {
            return;
        }

        if (looping) {
            BASS.BASS_ChannelFlags(channel, BASS.BASS_SAMPLE_LOOP, BASS.BASS_SAMPLE_LOOP);
        } else {
            BASS.BASS_ChannelFlags(channel, 0, BASS.BASS_SAMPLE_LOOP);
        }

        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, sampleInfo.freq * frequency);
    }
}
