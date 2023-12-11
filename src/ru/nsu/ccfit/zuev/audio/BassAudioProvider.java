package ru.nsu.ccfit.zuev.audio;

import android.util.Log;
import com.un4seen.bass.BASS;

/**
 * @author chenkang.ck
 */
public class BassAudioProvider {

    public static final int DEFAULT_FREQUENCY = 44100;

    public BassAudioProvider() {
        BASS.FloatValue freq = new BASS.FloatValue();
        freq.value = 1.0f;

        // This likely doesn't help, but also doesn't seem to cause any issues or any CPU increase.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5);

        // Reduce latency to a known sane minimum.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 5);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10);

        // Ensure there are no brief delays on audio operations (causing stream stalls etc.) after periods of silence.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_NONSTOP, 1);

        BASS.BASS_Init(-1, DEFAULT_FREQUENCY, BASS.BASS_DEVICE_LATENCY);

        Log.i("BASS-Config", "BASS initialized");
        Log.i("BASS-Config", "Update period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_UPDATEPERIOD));
        Log.i("BASS-Config", "Device period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_PERIOD));
        Log.i("BASS-Config", "Device buffer length:   " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_BUFFER));
        Log.i("BASS-Config", "Playback buffer length: " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER));
        Log.i("BASS-Config", "Device nonstop:         " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_NONSTOP));
    }

}
