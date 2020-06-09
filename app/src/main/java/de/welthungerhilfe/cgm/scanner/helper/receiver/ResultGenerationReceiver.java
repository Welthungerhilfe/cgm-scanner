package de.welthungerhilfe.cgm.scanner.helper.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;

public class ResultGenerationReceiver extends BroadcastReceiver {
    private int notificationID = 0;
    private static final String CHANNEL_ID = "CGM_Result_Generation_Notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    private void showNotification(Context context) {
        Notification.Builder notificationBuilder;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "CGM Result Generation", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new Notification.Builder(context, CHANNEL_ID);

        } else {
            notificationBuilder = new Notification.Builder(context);
        }

        notificationBuilder = notificationBuilder
                .setSmallIcon(R.drawable.icon_notif)
                .setContentIntent(pendingIntent)
                .setOngoing(false);

        notificationBuilder.setContentTitle("CGM Result Generation");
        notificationBuilder.setContentText("Height or weight result was generated.");

        notificationID++;
        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
