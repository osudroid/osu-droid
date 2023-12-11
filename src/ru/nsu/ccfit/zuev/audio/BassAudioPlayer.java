package ru.nsu.ccfit.zuev.audio;

import android.content.res.AssetManager;

/**
 * @author chenkang.ck
 */
public class BassAudioPlayer implements IMusicPlayer {

    private static BassAudioProvider provider = null;

    public static void initDevice() {
        if (provider == null) {
            provider = new BassAudioProvider();
        }
    }

}
