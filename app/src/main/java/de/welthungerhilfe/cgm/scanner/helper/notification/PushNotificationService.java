package de.welthungerhilfe.cgm.scanner.helper.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;

import de.welthungerhilfe.cgm.scanner.R;

public class PushNotificationService extends FirebaseMessagingService {
    private final String TAG = "NOTIFICATION";
    private final String CHANNEL_ID = "notification_channel";

    private static int notificationId = 0;



    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        notificationId ++;

        if (notificationId > 500)
            notificationId = 0;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        mBuilder.setContentTitle(getResources().getString(R.string.app_name));
        mBuilder.setVibrate(new long[] { 1000, 1000 });
        mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        mBuilder.setAutoCancel(true);


        if (remoteMessage.getData().size() > 0) {
            String id = "";
            double height = 0, weight = 0, muac = 0, head = 0;

            if (remoteMessage.getData().containsKey("id")) {
                id = remoteMessage.getData().get("id");
            }

            if (remoteMessage.getData().containsKey("height")) {
                try {
                    height = Double.parseDouble(remoteMessage.getData().get("height"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (remoteMessage.getData().containsKey("weight")) {
                try {
                    weight = Double.parseDouble(remoteMessage.getData().get("weight"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (remoteMessage.getData().containsKey("muac")) {
                try {
                    muac = Double.parseDouble(remoteMessage.getData().get("muac"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (remoteMessage.getData().containsKey("headCircumference")) {
                try {
                    head = Double.parseDouble(remoteMessage.getData().get("headCircumference"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Todo;
            //new OfflineTask().updateScanResult(id, height, weight, muac, head);

            mBuilder.setContentText("New scan result generated");

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pendingIntent);
        }

        if (remoteMessage.getNotification() != null) {
            mBuilder.setContentText(remoteMessage.getNotification().getBody());
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    @Override
    public void onNewToken(String token) {
        new SessionManager(this).setFcmToken(token);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Notification Channel", importance);
            channel.setDescription("Get scan result from Firebase");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
