/*
 * Written by Reco1l on 11/6/22 20:43
 * Last modified: 11/6/22 06:14
 */

package ru.nsu.ccfit.zuev.audio.serviceAudio;

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

import com.reco1l.osu.data.BeatmapInfo;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.R;

public class NotifyPlayer {

    public static int NOTIFICATION_ID = 1;

    private final MainActivity mActivity = GlobalManager.getInstance().getMainActivity();
    private Context context;

    private final String
            actionPrev = "player_previous",
            actionPlay = "player_play",
            actionNext = "Notify_next", //It has an usage in BassAudioFunc, that's why i left the original name.
            actionClose = "player_close";

    private PendingIntent prev, next, play, close;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat manager;
    public BroadcastReceiver receiver;
    public IntentFilter filter;

    private MediaSessionCompat mediaSession;
    private Notification notification;

    public boolean isShowing = false;
    private Bitmap defaultIcon;

    public void load(SongService service) {
        this.context = service.getApplicationContext();

        manager = NotificationManagerCompat.from(context);
        mediaSession = new MediaSessionCompat(context, "osu!droid");

        filter = new IntentFilter();
        filter.addAction(actionPrev);
        filter.addAction(actionPlay);
        filter.addAction(actionNext);
        filter.addAction(actionClose);

        defaultIcon = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.osut);

        prev = PendingIntent.getBroadcast(context, 0, new Intent(actionPrev), 0);
        next = PendingIntent.getBroadcast(context, 0, new Intent(actionNext), 0);
        play = PendingIntent.getBroadcast(context, 0, new Intent(actionPlay), 0);
        close = PendingIntent.getBroadcast(context, 0, new Intent(actionClose), 0);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (service.isRunningForeground())
                    return;

                switch (intent.getAction()) {
                    case actionPlay:
                        if (service.getStatus() == Status.PLAYING) service.pause();
                        else service.play();
                        break;
                    case actionPrev:
                        service.stop();
                        BeatmapInfo prevBeatmap = LibraryManager.selectPreviousBeatmapSet().getBeatmap(0);
                        service.preLoad(prevBeatmap.getAudioPath());
                        updateSong(prevBeatmap);
                        service.play();
                        break;
                    case actionNext:
                        service.stop();
                        BeatmapInfo nextBeatmap = LibraryManager.selectNextBeatmapSet().getBeatmap(0);
                        service.preLoad(nextBeatmap.getAudioPath());
                        updateSong(nextBeatmap);
                        service.play();
                        break;
                    case actionClose:
                        service.stop();
                        GlobalManager.getInstance().getMainScene().exit();
                        break;
                }
            }
        };
        create();
    }

    @SuppressLint("RestrictedApi")
    public void updateState() {
        if (!isShowing)
            return;
        boolean isPlaying = GlobalManager.getInstance().getSongService().getStatus() == Status.PLAYING;
        int drawable = isPlaying ? R.drawable.v_pause : R.drawable.v_play;

        builder.mActions.set(1, new NotificationCompat.Action(drawable, actionPlay, play));
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public void updateSong(BeatmapInfo beatmap) {
        if (!isShowing || beatmap == null)
            return;

        if (notification == null)
            create();

        Bitmap bitmap = null;

        if (beatmap.getBackgroundFilename() != null) {
            bitmap = BitmapFactory.decodeFile(beatmap.getBackgroundPath());
        }

        builder.setContentTitle(beatmap.getTitleText());
        builder.setContentText(beatmap.getArtistText());
        builder.setLargeIcon(bitmap != null ? bitmap : defaultIcon);

        notification = builder.build();
        manager.notify(NOTIFICATION_ID, notification);
    }

    public void show() {
        if (isShowing)
            return;
        if (notification == null)
            create();

        manager.notify(NOTIFICATION_ID, notification);
        isShowing = true;
    }

    public boolean hide() {
        if (!isShowing)
            return false;
        manager.cancel(NOTIFICATION_ID);
        isShowing = false;
        return true;
    }

    public void create() {
        String channelId = "ru.nsu.ccfit.zuev.audio";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                .setSmallIcon(R.drawable.notify_inso)
                .setLargeIcon(defaultIcon)
                .setContentTitle("title")
                .setContentText("artist")
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setShowWhen(false)
                .setContentIntent(openApp)
                .addAction(R.drawable.v_prev, actionPrev, prev)
                .addAction(R.drawable.v_play, actionPlay, play)
                .addAction(R.drawable.v_next, actionNext, next)
                .addAction(R.drawable.v_close, actionClose, close)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notification = builder.build();
    }

    public BroadcastReceiver getReceiver() {
        return receiver;
    }

    public IntentFilter getFilter() {
        return filter;
    }
}
