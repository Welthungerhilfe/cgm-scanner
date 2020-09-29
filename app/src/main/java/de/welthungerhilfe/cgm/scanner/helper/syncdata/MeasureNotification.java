package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import java.util.HashMap;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.utils.DataFormat;

public class MeasureNotification {

    private static final String CHANNEL_ID = "CGM_Result_Generation_Notification";
    private static HashMap<String, Integer> qr2notificationId = new HashMap<>();

    private Float height;
    private Float weight;
    private Long timestamp;

    public boolean hasHeight() {
        return height != null;
    }

    public boolean hasWeight() {
        return weight != null;
    }

    public void setHeight(float value) {
        height = value;
        timestamp = System.currentTimeMillis();
    }

    public void setWeight(float value) {
        weight = value;
        timestamp = System.currentTimeMillis();
    }

    public void showNotification(Context context, String qrCode) {
        if (timestamp == null) {
            return;
        }

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

        String title = String.format(context.getString(R.string.result_generation_at) + " %s", DataFormat.timestamp(context, DataFormat.TimestampFormat.DATE, timestamp));
        String text = "";
        if (hasHeight()) {
            text += context.getString(R.string.label_height).replace(":", "") + " ";
            text += context.getString(R.string.result_for) + " ";
            text += String.format(Locale.US, "%s : %.2f%s", qrCode, height, "cm");
        }
        if (hasWeight()) {
            if (!text.isEmpty()) {
                text += "\n";
            }
            text += context.getString(R.string.label_weight).replace(":", "") + " ";
            text += context.getString(R.string.result_for) + " ";
            text += String.format(Locale.US, "%s : %.2f%s", qrCode, weight, "kg");
        }
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);

        if (!qr2notificationId.containsKey(qrCode))
        {
            qr2notificationId.put(qrCode, qr2notificationId.size());
        }
        int notificationID = qr2notificationId.get(qrCode);
        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
