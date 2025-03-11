package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;

import androidx.core.app.NotificationManagerCompat;

import com.un4seen.bass.BASS;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;


public class SongService extends Service {
    public static int defaultFrequency = 44100;

    private BassAudioFunc audioFunc;
    private boolean isGaming = false;
    // private boolean isSettingMenu = false;
    private NotifyPlayer notify;

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
        if (notify == null) {
            notify = new NotifyPlayer();
            notify.load(this);
        }
        if (audioFunc == null) {
            audioFunc = new BassAudioFunc();

            registerReceiver(notify.getReceiver(), notify.getFilter());
            setReceiverStuff(notify.getReceiver(), notify.getFilter());
        }
        return new ReturnBindObject();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("Service unbind");
        hideNotification();
        NotificationManagerCompat.from(getApplicationContext()).cancelAll();
        exit();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        NotificationManagerCompat.from(getApplicationContext()).cancelAll();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManagerCompat.from(getApplicationContext()).cancelAll();
    }

    @Override
    public void onLowMemory() {
        NotificationManagerCompat.from(getApplicationContext()).cancelAll();
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
        notify.updateState();
    }

    public void pause() {
        if (audioFunc == null) return;
        audioFunc.pause();
        notify.updateState();
    }

    public boolean stop() {
        if (audioFunc == null) return false;
        notify.updateState();
        return audioFunc.stop();
    }

    public void stopWithoutNotify()
    {
        if (audioFunc != null)
        {
            audioFunc.stop();
        }
    }

    public boolean exit() {
        Log.w("SongService", "Hei Service is on EXIT()");
        if (audioFunc == null) return false;
        audioFunc.stop();
        audioFunc.unregisterReceiverBM();
        audioFunc.freeALL();
        unregisterReceiver(notify.getReceiver());
        stopSelf();
        return true;
    }

    public void seekTo(int time) {
        if (audioFunc == null) return;
        System.out.println(audioFunc.jump(time));
    }

    public boolean isGaming() {
        return isGaming;
    }

    public void setGaming(boolean isGaming) {
        audioFunc.setGaming(isGaming);
        if (!isGaming) {
            hideNotification();
        }
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

    public void showNotification() {
        if (this.isGaming) {
            Log.w("SongService", "NOT SHOW THE NOTIFY CUZ IS GAMING");
            return;
        }

        if (audioFunc != null) {
            audioFunc.onGamePause();
        }

        notify.show();
        notify.updateSong(GlobalManager.getInstance().getMainScene().getBeatmapInfo());
        notify.updateState();
    }

    public boolean hideNotification() {
        // Checking if the notification is shown is pretty hacky, but for now it is the case
        // only if the player leaves the game from the main menu, which is when we want to
        // reload BASS after being altered in `showNotification`.
        if (notify.isShowing && audioFunc != null) {
            audioFunc.onGameResume();
        }

        return notify.hide();
    }

    public void setReceiverStuff(BroadcastReceiver receiver, IntentFilter filter) {
        if (audioFunc != null) audioFunc.setReciverStuff(receiver, filter, this);
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
