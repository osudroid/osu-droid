package ru.nsu.ccfit.zuev.audio;

import android.content.res.AssetManager;

/**
 * @author chenkang.ck
 */
public class BassAudioPlayer implements IMusicPlayer {

    private static BassAudioProvider provider = null;
    private int loadMode = 0;
    private AssetManager manager;
    private String path;

    public BassAudioPlayer() {
        initDevice();
        provider.setUseSoftDecoder(0);
        provider.setDecoderMultiplier(100);
    }

    public BassAudioPlayer(final String fileName) {
        this();
        this.loadMode = 0;
        this.path = fileName;
    }

    public BassAudioPlayer(final AssetManager manager, String assetName) {
        this();
        this.loadMode = 1;
        this.manager = manager;
        this.path = assetName;
    }

    public static void initDevice() {
        if (provider == null) {
            provider = new BassAudioProvider();
        }
    }

    public static BassAudioProvider getProvider() {
        return provider;
    }

    public void prepare() {
        if (loadMode == 0) {
            provider.prepare(path);
        } else {
            provider.prepare(manager, path);
        }
    }

    public void prepare(String fileName) {
        provider.prepare(fileName);
    }

    public void play() {
        if (provider != null) {
            provider.play();
        }
    }

    public void pause() {
        if (provider != null) {
            provider.pause();
        }
    }

    public void stop() {
        if (provider != null) {
            provider.stop();
        }
    }

    public void release() {
        if (provider != null) {
            provider.stop();
            provider.free();
        }
    }

    public Status getStatus() {
        if (provider != null) {
            return provider.getStatus();
        }
        return Status.STALLED;
    }

    public int getPosition() {
        if (provider != null) {
            return (int) (provider.getPosition() * 1000.0);
        }
        return 0;
    }

    public int getLength() {
        if (provider != null) {
            return (int) (provider.getLength() * 1000.0);
        }
        return 0;
    }

    public float[] getSpectrum() {
        if (provider != null) {
            return provider.getSpectrum();
        }
        return new float[0];
    }

    public void seekTo(int ms) {
        if (provider != null) {
            provider.seek(ms / 1000.0);
        }
    }

    public void setUseSoftDecoder(int decoder) {
        if (provider != null) {
            provider.setUseSoftDecoder(decoder);
        }
    }

    public void setDecoderMultiplier(int multiplier) {
        if (provider != null) {
            provider.setDecoderMultiplier(multiplier);
        }
    }

    public void setLoop() {
        if (provider != null) {
            provider.setLoop();
        }
    }

    public float getVolume() {
        if (provider != null) {
            return provider.getVolume();
        }
        return 0;
    }

    public void setVolume(float volume) {
        if (provider != null) {
            provider.setVolume(volume);
        }
    }

    public int getErrorCode() {
        if (provider != null) {
            return provider.getErrorCode();
        }
        return -1;
    }
}
