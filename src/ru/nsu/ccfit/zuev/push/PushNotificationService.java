package ru.nsu.ccfit.zuev.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator;

/**
 * @author kairusds
 */
public class PushNotificationService extends FirebaseMessagingService {

    public static int notificationCount = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if(remoteMessage.getData().size() > 0) {
            HashMap<String, String> data = new HashMap<String, String>(remoteMessage.getData());
            String channelId = "ru.nsu.ccfit.zuev.push";
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            String title = data.get("title");
            if(title == null) title = "osu!droid";
            String message = data.get("message");
            if(message == null) message = "error";
            String url = data.get("url");
            String imageUrl = data.get("imageUrl");

            NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.notify_inso)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri);

            if(!imageUrl.isEmpty()) {
                String filePath = getCacheDir().getPath() + "/" + MD5Calcuator.getStringMD5("osuplus" + imageUrl);
                boolean downloaded = OnlineFileOperator.downloadFile(imageUrl, filePath);
                if(downloaded) {
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    notificationBuilder.setLargeIcon(bitmap)
                        .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null));
                }
            }

            if(!url.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
                notificationBuilder.setContentIntent(pendingIntent);
            }else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
                notificationBuilder.setContentIntent(pendingIntent);
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId,
                    "osu!droid Push Notfications",
                    NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("osu!droid Push Notfications");
                notificationManager.createNotificationChannel(channel);
            }

            int notificationId = notificationCount++;
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

}