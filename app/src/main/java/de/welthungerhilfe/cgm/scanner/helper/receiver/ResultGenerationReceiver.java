package de.welthungerhilfe.cgm.scanner.helper.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;

public class ResultGenerationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "CGM_Result_Generation_Notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        String qrCode = intent.getStringExtra("qr_code");
        long timestamp = intent.getLongExtra("received_at", 0);
        double weight = intent.getDoubleExtra("weight", 0);
        double height = intent.getDoubleExtra("height", 0);

        if (qrCode != null && weight != 0)
            showNotification(context, qrCode, "weight", weight, timestamp);
        if (qrCode != null && height != 0)
            showNotification(context, qrCode, "height", height, timestamp);
    }

    private void showNotification(Context context, String qrCode, String type, double value, long timestamp) {
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
                .setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0, 500, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOngoing(false);

        notificationBuilder.setContentTitle(String.format(context.getString(R.string.result_generation_at) + " %s", DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE, timestamp)));
        notificationBuilder.setContentText(String.format(Locale.US, "%s " + context.getString(R.string.result_for) + " %s : %.2f%s", type, qrCode, value, type.equals("weight") ? "kg" : "cm"));

        int notificationID = Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(new Date()));
        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
