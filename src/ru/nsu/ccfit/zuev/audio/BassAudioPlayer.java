package ru.nsu.ccfit.zuev.audio;

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
