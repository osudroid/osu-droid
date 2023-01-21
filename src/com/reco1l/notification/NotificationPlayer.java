package com.reco1l.notification;

import static androidx.media.app.NotificationCompat.MediaStyle;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.reco1l.Game;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 11/6/22 20:43

public class NotificationPlayer {

    public boolean isShowing = false;

    private final static String
            ACTION_PREV = "player_previous",
            ACTION_PLAY = "player_play",
            ACTION_NEXT = "Notify_next",
            ACTION_CLOSE = "player_close";

    private PendingIntent prev, next, play, close;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat manager;
    public BroadcastReceiver receiver;
    public IntentFilter filter;

    private Context context;
    private Bitmap defaultIcon;
    private Notification notification;
    private MediaSessionCompat mediaSession;

    //--------------------------------------------------------------------------------------------//

    public void load(SongService service) {
        this.context = service.getApplicationContext();

        manager = NotificationManagerCompat.from(context);
        mediaSession = new MediaSessionCompat(context, "osu!droid");

        filter = new IntentFilter();
        filter.addAction(ACTION_PREV);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_CLOSE);

        defaultIcon = Game.bitmapManager.get("menu-background");

        prev = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PREV), 0);
        next = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_NEXT), 0);
        play = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_PLAY), 0);
        close = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_CLOSE), 0);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (service.isRunningForeground())
                    return;

                switch (intent.getAction()) {
                    case ACTION_PLAY:
                        if (service.getStatus() == Status.PLAYING) service.pause();
                        else service.play();
                        break;
                    case ACTION_PREV:
                        service.stop();
                        BeatmapInfo prevBeatmap = LibraryManager.getInstance().getPrevBeatmap();
                        service.preLoad(prevBeatmap.getMusic());
                        updateSong(prevBeatmap);
                        service.play();
                        break;
                    case ACTION_NEXT:
                        service.stop();
                        BeatmapInfo nextBeatmap = LibraryManager.getInstance().getNextBeatmap();
                        service.preLoad(nextBeatmap.getMusic());
                        updateSong(nextBeatmap);
                        service.play();
                        break;
                    case ACTION_CLOSE:
                        service.stop();
                        Game.activity.exit();
                        break;
                }
            }
        };
        create();
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressLint("RestrictedApi")
    public void updateState() {
        if (!isShowing)
            return;
        boolean isPlaying = GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING;
        int drawable = isPlaying ? R.drawable.v_pause : R.drawable.v_play;

        builder.mActions.set(1, new NotificationCompat.Action(drawable, ACTION_PLAY, play));
        manager.notify(1, builder.build());
    }

    public void updateSong(BeatmapInfo beatmap) {
        if (!isShowing || beatmap == null)
            return;

        if (notification == null)
            create();

        Bitmap bitmap = null;
        String title = " ";
        String artist = " ";

        if (beatmap.getArtistUnicode() != null && beatmap.getTitleUnicode() != null) {
            title = beatmap.getTitleUnicode();
            artist = beatmap.getArtistUnicode();
        }
        else if (beatmap.getArtist() != null && beatmap.getTitle() != null) {
            title = beatmap.getTitle();
            artist = beatmap.getArtist();
        }

        if (beatmap.getTrack(0).getBackground() != null) {
            bitmap = BitmapFactory.decodeFile(beatmap.getTrack(0).getBackground());
        }

        builder.setContentTitle(title);
        builder.setContentText(artist);
        builder.setLargeIcon(bitmap != null ? bitmap : defaultIcon);

        notification = builder.build();
        manager.notify(1, notification);
    }

    //--------------------------------------------------------------------------------------------//

    public void show() {
        if (isShowing)
            return;
        if (notification == null)
            create();

        manager.notify(1, notification);
        isShowing = true;
    }

    public boolean hide() {
        if (!isShowing)
            return false;
        manager.cancel(1);
        isShowing = false;
        return true;
    }

    public void create() {
        String channelId = "ru.nsu.ccfit.zuev.audio";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Beatmap music player for osu!droid",
                    NotificationManager.IMPORTANCE_LOW);

            channel.setDescription("osu!droid music player");
            manager.createNotificationChannel(channel);
        }

        //This disables progress bar area
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .build();
        mediaSession.setMetadata(metadata);

        PendingIntent openApp =
                PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.v_app_icon)
                .setLargeIcon(defaultIcon)
                .setContentTitle("title")
                .setContentText("artist")
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setOngoing(true)
                .setShowWhen(false)
                .setContentIntent(openApp)
                .addAction(R.drawable.v_prev, ACTION_PREV, prev)
                .addAction(R.drawable.v_play, ACTION_PLAY, play)
                .addAction(R.drawable.v_next, ACTION_NEXT, next)
                .addAction(R.drawable.v18_close, ACTION_CLOSE, close)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notification = builder.build();
    }

    //--------------------------------------------------------------------------------------------//

    public BroadcastReceiver getReceiver() {
        return receiver;
    }

    public IntentFilter getFilter() {
        return filter;
    }
}
