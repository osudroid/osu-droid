package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.R;


public class SongService extends Service {

    private BassAudioFunc audioFunc;
    private NotificationManager manager;
    private Notification notification;
    private RemoteViews notifyView_Small;
    private boolean showingNotify = false;
    private String backgroundPath = " ";
    private boolean isGaming = false;
    // private boolean isSettingMenu = false;
    private IntentFilter filter = null;
    private BroadcastReceiver onNotifyButtonClick = null;
    private long lastHit = 0;
    private static final String CHANNEL_ID = "ru.nsu.ccfit.zuev.audio";

    @Override
    public IBinder onBind(Intent intent) {
        if (audioFunc == null) {
            audioFunc = new BassAudioFunc();
            if (Build.VERSION.SDK_INT > 10) onCreateNotifyReceiver();
        }
        return new ReturnBindObject();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("Service unbind");
        hideNotifyPanel();
        exit();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        System.out.println("onReBind");
    }

    public boolean preLoad(String filePath, PlayMode mode, boolean isLoop) {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) return false;
            if (isLoop) {
                audioFunc.setLoop(isLoop);
            }
            return audioFunc.preLoad(filePath, mode);
        }
        return false;
    }

    public boolean preLoad(String filePath) {
        return preLoad(filePath, PlayMode.MODE_NONE, false);
    }

    public boolean preLoad(String filePath, PlayMode mode) {
        return preLoad(filePath, mode, false);
    }

    public boolean preLoad(String filePath, float speed, boolean enableNC) {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) return false;
                audioFunc.setLoop(false);
            return audioFunc.preLoad(filePath, speed, enableNC);
        }
        return false;
    }

    public boolean preLoadWithLoop(String filePath) {
        return preLoad(filePath, PlayMode.MODE_NONE, true);
    }

    public void play() {
        if (audioFunc == null) return;
        audioFunc.play();
        updateStatus();
    }

    public void pause() {
        if (audioFunc == null) return;
        audioFunc.pause();
        updateStatus();
    }

    public boolean stop() {
        if (audioFunc == null) return false;
        updateStatus();
        return audioFunc.stop();
    }

    public boolean exit() {
        Log.w("SongService", "Hei Service is on EXIT()");
        if (audioFunc == null) return false;
        audioFunc.stop();
        audioFunc.unregisterReceiverBM();
        audioFunc.freeALL();
        if (Build.VERSION.SDK_INT > 10) {
            unregisterReceiver(onNotifyButtonClick);
            stopForeground(true);
        }
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
        if (!isGaming && showingNotify) {
            hideNotifyPanel();
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

    //Notify相关处理
    public void showNotifyPanel() {
        if (this.isGaming) {
            Log.w("SongService", "NOT SHOW THE NOTIFY CUZ IS GAMING");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "osu!droid AudioService", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("AudioService for osu!droid");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (notification == null) createNotifyPanel();
        startForeground(9, notification);
        manager.notify(9, notification);
        showingNotify = true;
    }

    public boolean hideNotifyPanel() {
        if (!showingNotify) return false;
        if (notification != null) {
            showingNotify = false;
        }
        stopForeground(true);
        return true;
    }

    public void setReceiverStuff(BroadcastReceiver receiver, IntentFilter filter) {
        if (audioFunc != null) audioFunc.setReciverStuff(receiver, filter, this);
    }

    public void updateTitleText(String title, String artist) {
        if (notification == null) return;
        notifyView_Small.setTextViewText(R.id.notify_small_title, title);
        notifyView_Small.setTextViewText(R.id.notify_small_artist, artist);
        showNotifyPanel();
    }

    public void updateStatus() {
        if (notification == null) return;
        switch (getStatus()) {
            case PLAYING:
                notifyView_Small.setImageViewResource(R.id.notify_small_play, R.drawable.notify_pause);
                break;
            case PAUSED:
                notifyView_Small.setImageViewResource(R.id.notify_small_play, R.drawable.notify_play);
                break;
            case STOPPED:
                notifyView_Small.setImageViewResource(R.id.notify_small_play, R.drawable.notify_stop);
                break;
        }
        showNotifyPanel();
    }

    public void updateCoverImage(String backgroundPath) {
        if (notification == null) return;
        if (backgroundPath == null || backgroundPath.trim().equals("") || !checkFileExist(backgroundPath)) {
            notifyView_Small.setImageViewResource(R.id.notify_small_icon, R.drawable.osut);
            return;
        }
        this.backgroundPath = backgroundPath;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        if(Config.isSafeBeatmapBg()) {
            File bg;
            if ((bg = new File(Config.getSkinPath() + "menu-background.png")).exists()
                    || (bg = new File(Config.getSkinPath() + "menu-background.jpg")).exists()) {
                notifyView_Small.setImageViewBitmap(R.id.notify_small_icon, BitmapFactory.decodeFile(bg.getAbsolutePath(), options));
            }else {
                notifyView_Small.setImageViewResource(R.id.notify_small_icon, R.drawable.osut);
            }
        }else {
            notifyView_Small.setImageViewBitmap(R.id.notify_small_icon, BitmapFactory.decodeFile(backgroundPath, options));  
        }
        
        showNotifyPanel();
    }

    private void createNotifyPanel() {
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //创建Notify的布局
        notifyView_Small = new RemoteViews(getPackageName(), R.layout.notify_small_layout);
        notifyView_Small.setImageViewResource(R.id.notify_small_icon, R.drawable.notify_icon);

        Intent intent = new Intent(SongService.this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        //Notify按钮的事件设置
        notifyView_Small.setOnClickPendingIntent(R.id.notify_small_up, PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("Notify_up"), 0));
        notifyView_Small.setOnClickPendingIntent(R.id.notify_small_play, PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("Notify_play"), 0));
        notifyView_Small.setOnClickPendingIntent(R.id.notify_small_next, PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("Notify_next"), 0));
        notifyView_Small.setOnClickPendingIntent(R.id.notify_small_cancel, PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent("Notify_cancel"), 0));
        notifyView_Small.setOnClickPendingIntent(R.id.notify_small_icon, pendingIntent);

        //开始创建Notify
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setTicker("Background playing~ ///w///")
            .setSmallIcon(R.drawable.notify_inso)
            .setOngoing(true);
        notification = builder.build();
        notification.contentView = notifyView_Small;
        //notification.contentIntent = pendingIntent;
        notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
    }

    public boolean checkFileExist(String path) {
        if (path == null) return false;
        if (path.trim().equals("")) return false;
        else {
            File songFile = new File(path);
            if (!songFile.exists()) return false;
        }
        return true;
    }

    private void onCreateNotifyReceiver() {
        if (this.filter == null) {
            filter = new IntentFilter();
            filter.addAction("Notify_up");
            filter.addAction("Notify_play");
            filter.addAction("Notify_pause");
            filter.addAction("Notify_stop");
            filter.addAction("Notify_next");
        }

        onNotifyButtonClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isRunningForeground()) return;
                if (lastHit == 0) {
                    lastHit = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - lastHit <= 1000) {
                        return;
                    }
                }
                lastHit = System.currentTimeMillis();
                if (intent.getAction().equals("Notify_play")) {
                    if (getStatus() == Status.PLAYING) pause();
                    else {
                        play();
                    }
                } else if (intent.getAction().equals("Notify_next")) {
                    if (isRunningForeground()) return;

                    stop();
                    BeatmapInfo tempBeatmap = LibraryManager.getInstance().getNextBeatmap();
                    preLoad(tempBeatmap.getMusic());
                    updateCoverImage(tempBeatmap.getTrack(0).getBackground());

                    if (tempBeatmap.getArtistUnicode() != null && tempBeatmap.getTitleUnicode() != null && !Config.isForceRomanized()) {
                        updateTitleText(tempBeatmap.getTitleUnicode(), tempBeatmap.getArtistUnicode());
                    }else if (tempBeatmap.getArtist() != null && tempBeatmap.getTitle() != null && Config.isForceRomanized()) {
                        updateTitleText(tempBeatmap.getTitle(), tempBeatmap.getArtist());
                    }

                    play();
                } else {
                    if (isRunningForeground()) return;
                    stop();
                    BeatmapInfo tempBeatmap = LibraryManager.getInstance().getPrevBeatmap();
                    preLoad(tempBeatmap.getMusic());
                    updateCoverImage(tempBeatmap.getTrack(0).getBackground());

                    if (tempBeatmap.getArtistUnicode() != null && tempBeatmap.getTitleUnicode() != null && !Config.isForceRomanized()) {
                        updateTitleText(tempBeatmap.getTitleUnicode(), tempBeatmap.getArtistUnicode());
                    }else if (tempBeatmap.getArtist() != null && tempBeatmap.getTitle() != null && Config.isForceRomanized()) {
                        updateTitleText(tempBeatmap.getTitle(), tempBeatmap.getArtist());
                    }

                    play();
                }
            }
        };

        registerReceiver(onNotifyButtonClick, filter);

        setReceiverStuff(onNotifyButtonClick, filter);
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
