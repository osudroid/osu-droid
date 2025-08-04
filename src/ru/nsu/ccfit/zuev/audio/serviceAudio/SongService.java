package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

import com.un4seen.bass.BASS;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.MainActivity;


public class SongService extends Service {
    public static int defaultFrequency = 44100;

    private BassAudioFunc audioFunc;
    private boolean isGaming = false;
    // private boolean isSettingMenu = false;

    public static void initBASS() {
        // This likely doesn't help, but also doesn't seem to cause any issues or any CPU increase.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5);

        // Reduce latency to a known sane minimum.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_PERIOD, 5);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 10);

        // Ensure there are no brief delays on audio operations (causing stream stalls etc.) after periods of silence.
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_NONSTOP, 1);

        BASS.BASS_Init(-1, defaultFrequency, BASS.BASS_DEVICE_LATENCY);

        Log.i("BASS-Config", "BASS initialized");
        Log.i("BASS-Config", "Update period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_UPDATEPERIOD));
        Log.i("BASS-Config", "Device period:          " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_PERIOD));
        Log.i("BASS-Config", "Device buffer length:   " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_BUFFER));
        Log.i("BASS-Config", "Playback buffer length: " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_BUFFER));
        Log.i("BASS-Config", "Device nonstop:         " + BASS.BASS_GetConfig(BASS.BASS_CONFIG_DEV_NONSTOP));
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (audioFunc == null) {
            audioFunc = new BassAudioFunc();
        }
        return new ReturnBindObject();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("Service unbind");
        exit();
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        System.out.println("onReBind");
    }

    public boolean preLoad(String filePath) {
        return preLoad(filePath, 1, false, false);
    }

    public boolean preLoad(String filePath, float speed, boolean adjustPitch) {
        return preLoad(filePath, speed, adjustPitch, false);
    }

    public boolean preLoad(String filePath, float speed, boolean adjustPitch, boolean isLoop) {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) {
                return false;
            }

            audioFunc.setLoop(isLoop);
            return audioFunc.preLoad(filePath, speed, adjustPitch);
        }
        return false;
    }

    public void play() {
        if (audioFunc == null) return;
        audioFunc.play();
    }

    public void pause() {
        if (audioFunc == null) return;
        audioFunc.pause();
    }

    public boolean stop() {
        if (audioFunc == null) return false;
        return audioFunc.stop();
    }

    public boolean exit() {
        Log.w("SongService", "Hei Service is on EXIT()");
        if (audioFunc == null) return false;
        audioFunc.stop();
        audioFunc.freeALL();
        stopSelf();
        return true;
    }

    public void seekTo(int time) {
        if (audioFunc == null) return;
        Log.i("BASS", "Seeking to " + time + " ms: " + (audioFunc.jump(time) ? "Success" : "Failed"));
    }

    public boolean isGaming() {
        return isGaming;
    }

    public void setGaming(boolean isGaming) {
        audioFunc.setGaming(isGaming);
        Log.w("Gaming Mode", "In Gamming mode :" + isGaming);
        this.isGaming = isGaming;
    }

    /*
    public boolean isSettingMenu() {
        return isSettingMenu;
    }

    public void setIsSettingMenu(boolean isSettingMenu) {
        this.isSettingMenu = isSettingMenu;
    } */

    public Status getStatus() {
        if (audioFunc != null) {
            return audioFunc.getStatus();
        }
        return Status.STOPPED;
    }

    public int getPosition() {
        if (audioFunc != null) {
            return audioFunc.getPosition();
        }
        return 0;
    }

    public int getLength() {
        if (audioFunc != null) {
            return audioFunc.getLength();
        }
        return 0;
    }

    public float[] getSpectrum() {
        if (audioFunc != null) {
            return audioFunc.getSpectrum();
        }
        return new float[0];
    }

    public float getVolume() {
        if (audioFunc != null) {
            return audioFunc.getVolume();
        }
        return 0;
    }

    public void setVolume(float volume) {
        if (audioFunc != null) {
            audioFunc.setVolume(volume);
        }
    }

    public void setSpeed(float speed) {
        if (audioFunc != null) {
            audioFunc.setSpeed(speed);
        }
    }

    public void setAdjustPitch(boolean adjustPitch) {
        if (audioFunc != null) {
            audioFunc.setAdjustPitch(adjustPitch);
        }
    }

    public void setFrequencyForcefully(float frequency) {
        if (audioFunc != null) {
            audioFunc.setFrequencyForcefully(frequency);
        }
    }

    public float getFrequency() {
        if (audioFunc != null) {
            return audioFunc.getFrequency();
        }
        return 0f;
    }

    public boolean checkFileExist(String path) {
        if (path == null) return false;
        if (path.trim().isEmpty()) return false;
        else {
            File songFile = new File(path);
            if (!songFile.exists()) return false;
        }
        return true;
    }

    public boolean isRunningForeground() {
        return MainActivity.isActivityVisible();
    }

    public class ReturnBindObject extends Binder {
        public SongService getObject() {
            return SongService.this;
        }
    }

}
