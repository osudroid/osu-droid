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

    public static void initDevice() {
        if (provider == null) {
            provider = new BassAudioProvider();
        }
    }

}
